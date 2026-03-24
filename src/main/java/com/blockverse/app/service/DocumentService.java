package com.blockverse.app.service;



import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.*;
import com.blockverse.app.entity.*;
import com.blockverse.app.enums.*;
import com.blockverse.app.exception.DocumentException;
import com.blockverse.app.exception.DocumentNotFoundException;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.WorkSpaceNotFoundException;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.notification.NotificationService;
import com.blockverse.app.repo.*;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentRepo documentRepo;
    private final WorkSpaceRepo workSpaceRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final SecurityUtil securityUtil;
    private final BlockService blockService;
    private final AuditLogService auditLogService;
    private final BlockRepo blockRepo;
    private final BlockChangeLogRepo blockChangeLogRepo;
    private final DocumentSocketPublisher documentSocketPublisher;
    private final DocumentShareRepo documentShareRepo;
    private final NotificationService notificationService;
    private final RateLimiterService rateLimiterService;
    
    private String generateToken(){
        return UUID.randomUUID().toString();
    }

    private WorkSpace getWorkSpaceOrThrow(int workspaceId) {
        return workSpaceRepo.findById(workspaceId)
                .orElseThrow(() -> new WorkSpaceNotFoundException("Workspace not found"));
    }

    private Document getDocumentOrThrow(int documentId) {
        return documentRepo.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
    }

    private WorkSpaceMember getMembershipOrThrow(User user, WorkSpace workSpace) {
        return workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace)
                .orElseThrow(() -> new InsufficientPermissionException("User is not a member of this workspace"));
    }

    public DocumentResponse createDocument(int workspaceId, CreateDocumentRequest request) {

        User currentUser = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(currentUser.getId(), "CREATE_DOCUMENT");

        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);

        WorkSpaceMember membership = getMembershipOrThrow(currentUser, workSpace);

        Document document = new Document();
        if(request.getTitle() == null || request.getTitle().isBlank()){
            throw new DocumentException("Title cannot be empty");
        }
        document.setTitle(request.getTitle());
        document.setWorkSpace(workSpace);
        document = documentRepo.save(document);

        auditLogService.auditLog(document.getWorkSpace().getId(),
                currentUser.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_CREATED,
                "Document created with title: " + document.getTitle()
        );

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.CREATE,
                        document.getId()
                )
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(workSpace)
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != currentUser.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "New document created: " + document.getTitle(),
                NotificationType.CREATE,
                document.getId());

        return documentMapper.toResponse(document);
    }

    public DocumentResponse getDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();

        Document document = documentRepo.findByIdAndArchivedFalseAndDeletedFalse(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found or is archived/deleted"));

        getMembershipOrThrow(user, document.getWorkSpace());

        return documentMapper.toResponse(document);
    }

    public DocumentDetailsResponse getDocumentWithBlocks(int documentId) {
        User user = securityUtil.getLoggedInUser();

        Document document = getDocumentOrThrow(documentId);

        getMembershipOrThrow(user, document.getWorkSpace());

        List<BlockResponse> blocks = blockService.getBlocksForDocument(documentId);

        return DocumentDetailsResponse.builder()
                .document(documentMapper.toResponse(document))
                .blocks(blocks)
                .build();
    }

    public DocumentResponse updateDocument(int documentId, UpdateDocumentRequest request) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "UPDATE_DOCUMENT");

        Document document = getDocumentOrThrow(documentId);
        if(document.isArchived() || document.isDeleted()) {
            throw new DocumentNotFoundException("Document not found or is archived/deleted");
        }

        getMembershipOrThrow(user, document.getWorkSpace());

        if(request.getTitle() == null || request.getTitle().isBlank()){
            throw new DocumentException("Title cannot be empty");
        }
        document.setTitle(request.getTitle());
        documentRepo.save(document);

        auditLogService.auditLog(
                document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_UPDATED,
                "Document updated with new title: " + document.getTitle()
        );

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.UPDATE,
                        document.getId()
                )
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "Document updated: " + document.getTitle(),
                NotificationType.UPDATE,
                documentId);

        return documentMapper.toResponse(document);
    }

    public List<DocumentResponse> getDocumentsByWorkspace(int workspaceId) {
        User user = securityUtil.getLoggedInUser();

        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);

        WorkSpaceMember membership = getMembershipOrThrow(user, workSpace);

        List<Document> documents = documentRepo.findByWorkSpaceAndArchivedFalseAndDeletedFalseOrderByCreatedAtDesc(workSpace);

        return documents.stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    public void archiveDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "ARCHIVE_DOCUMENT");

        Document document = documentRepo
                .findByIdAndArchivedFalseAndDeletedFalse(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());

        if (membership.getRole() != WorkSpaceRole.OWNER && membership.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can archive documents");
        }

        auditLogService.auditLog(document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_ARCHIVED,
                "Document archived with title: " + document.getTitle()
        );

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.ARCHIVE,
                        document.getId()
                )
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "Document archived: " + document.getTitle(),
                NotificationType.ARCHIVE,
                documentId);

        document.setArchived(true);
        documentRepo.save(document);
    }

    public void unarchiveDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "UNARCHIVE_DOCUMENT");

        Document document = documentRepo
                .findByIdAndArchivedTrueAndDeletedFalse(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());

        if (membership.getRole() != WorkSpaceRole.OWNER && membership.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can unarchive documents");
        }
        auditLogService.auditLog(document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_UNARCHIVED,
                "Document unarchived with title: " + document.getTitle()
        );

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.UNARCHIVE,
                        document.getId()
                )
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "Document unarchived: " + document.getTitle(),
                NotificationType.UNARCHIVE,
                documentId);

        document.setArchived(false);
        documentRepo.save(document);
    }

    public void deleteDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "DELETE_DOCUMENT");
        Document document = documentRepo
                .findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());

        if (membership.getRole() != WorkSpaceRole.OWNER && membership.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can delete documents");
        }

        if(document.isDeleted()){
            throw new DocumentException("Document already in trash");
        }

        auditLogService.auditLog(document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_DELETED,
                "Document deleted with title: " + document.getTitle()
        );

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.DELETE,
                        document.getId()
                )
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "Document moved to trash: " + document.getTitle(),
                NotificationType.DELETE,
                documentId);

        document.setDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(user.getId());
        documentRepo.save(document);
    }

    public void restoreDeletedDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "RESTORE_DOCUMENT");
        Document document = getDocumentOrThrow(documentId);
        WorkSpaceMember member = getMembershipOrThrow(user, document.getWorkSpace());

        if(member.getRole() != WorkSpaceRole.OWNER && member.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can restore documents");
        }

        if (!document.isDeleted()) {
            throw new DocumentException("Document is not in trash");
        }

        auditLogService.auditLog(document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_RESTORED,
                "Document restored with title: " + document.getTitle());

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.RESTORE,
                        document.getId()
                )
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "Document restored from trash: " + document.getTitle(),
                NotificationType.RESTORE,
                documentId);

        document.setDeleted(false);
        document.setDeletedAt(null);
        document.setDeletedBy(null);
        documentRepo.save(document);
    }

    public void permanentDeleteDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "PERMANENT_DELETE_DOCUMENT");
        Document document = getDocumentOrThrow(documentId);
        WorkSpaceMember member = getMembershipOrThrow(user, document.getWorkSpace());

        if (member.getRole() != WorkSpaceRole.OWNER && member.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException(
                    "Only workspace owners or admin can permanently delete documents");
        }

        if(!document.isDeleted()){
            throw new DocumentException("Document must be in trash before permanent deletion");
        }

        auditLogService.auditLog(document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_PERMANENTLY_DELETED,
                "Document permanently deleted with title: " + document.getTitle()
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();

        documentSocketPublisher.broadcast(
                document.getId(),
                new DocumentEvent(
                        document.getId(),
                        AuditEntityType.DOCUMENT,
                        DocumentOperationType.PERMANENT_DELETE,
                        document.getId()
                )
        );

        notificationService.sendBulkNotification(recipientIds,
                "Document permanently deleted: " + document.getTitle(),
                NotificationType.PERMANENT_DELETE,
                documentId);

        blockChangeLogRepo.deleteByDocument(document);
        documentShareRepo.deleteByDocument(document);
        List<com.blockverse.app.entity.Block> rootBlocks = blockRepo.findByDocumentAndParentIsNull(document);
        blockRepo.deleteAll(rootBlocks);
        documentRepo.delete(document);
    }

    public List<DocumentResponse> getTrashDocumentsByWorkspace(int workspaceId) {
        User user = securityUtil.getLoggedInUser();
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);
        getMembershipOrThrow(user, workSpace);

        List<Document> documents = documentRepo.findByWorkSpaceAndDeletedTrue(workSpace);

        return documents.stream()
                .map(documentMapper::toResponse)
                .toList();
    }
    
    public ShareLinkResponse createShareLink(int documentId, int expiryMinutes) {
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "CREATE_SHARE_LINK");
        Document document = getDocumentOrThrow(documentId);
        WorkSpaceMember member = getMembershipOrThrow(user, document.getWorkSpace());

        if(member.getRole() != WorkSpaceRole.OWNER && member.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can create share links");
        }

        String token = generateToken();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(expiryMinutes);

        DocumentShare share = DocumentShare.builder()
                .document(document)
                .token(token)
                .expiryTime(expiryTime)
                .createdBy(user.getId())
                .active(true)
                .build();
        
        documentShareRepo.save(share);

        auditLogService.auditLog(
                document.getWorkSpace().getId(),
                user.getId(),
                AuditEntityType.DOCUMENT,
                document.getId(),
                AuditActionType.DOCUMENT_SHARED,
                "{\"expiryMinutes\": " + expiryMinutes + "}"
        );

        List<Integer> recipientIds = workSpaceMemberRepo.findByWorkSpaceAndDeletedAtIsNull(document.getWorkSpace())
                .stream()
                .map(wsm -> wsm.getUser().getId())
                .filter(id -> id != user.getId())
                .toList();
        notificationService.sendBulkNotification(recipientIds,
                "A share link was created for document: " + document.getTitle(),
                NotificationType.SHARE,
                documentId);

        return new ShareLinkResponse("http://localhost:8080/share/" + token,
                expiryTime);
    }
}