package com.blockverse.app.service;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.CreateDocumentRequest;
import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.dto.document.UpdateDocumentRequest;
import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.entity.WorkSpaceMember;
import com.blockverse.app.enums.WorkSpaceRole;
import com.blockverse.app.exception.DocumentNotFoundException;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.WorkSpaceNotFoundException;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.repo.DocumentRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private DocumentMapper documentMapper;
    private final DocumentRepo documentRepo;
    private final WorkSpaceRepo workSpaceRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final SecurityUtil securityUtil;
    private final BlockService blockService;

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
                .orElseThrow(() -> new WorkSpaceNotFoundException("User is not a member of this workspace"));
    }

    public DocumentResponse createDocument(int workspaceId, CreateDocumentRequest request) {

        User currentUser = securityUtil.getLoggedInUser();

        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);

        WorkSpaceMember membership = getMembershipOrThrow(currentUser, workSpace);

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setWorkSpace(workSpace);
        documentRepo.save(document);

        return documentMapper.toResponse(document);
    }
    
    public DocumentResponse getDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        
        Document document = getDocumentOrThrow(documentId);
        
        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());

        return documentMapper.toResponse(document);
    }
    
    public DocumentDetailsResponse getDocumentWithBlocks(int documentId) {
        User user = securityUtil.getLoggedInUser();
        
        Document document = getDocumentOrThrow(documentId);
        
        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());
        
        List<BlockResponse> blocks = blockService.getBlocksForDocument(documentId);

        return DocumentDetailsResponse.builder()
                .document(documentMapper.toResponse(document))
                .blocks(blocks)
                .build();
    }
    
    public DocumentResponse updateDocument(int documentId, UpdateDocumentRequest request) {
        User user = securityUtil.getLoggedInUser();
        
        Document document = getDocumentOrThrow(documentId);

        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());

        document.setTitle(request.getTitle());
        documentRepo.save(document);

        return documentMapper.toResponse(document);
    }
    
    public List<DocumentResponse> getDocumentsByWorkspace(int workspaceId) {
        User user = securityUtil.getLoggedInUser();
        
        WorkSpace workSpace = getWorkSpaceOrThrow(workspaceId);

        WorkSpaceMember membership = getMembershipOrThrow(user, workSpace);

        List<Document> documents = documentRepo.findByWorkSpaceAndArchivedFalse(workSpace);

        return documents.stream()
                .map(documentMapper::toResponse)
                .toList();
    }
    
    public void archiveDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        
        Document document = getDocumentOrThrow(documentId);

        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());
        
        if(membership.getRole() != WorkSpaceRole.OWNER || membership.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can archive documents");
        }

        document.setArchived(true);
        documentRepo.save(document);
    }
    
    public void unarchiveDocument(int documentId) {
        User user = securityUtil.getLoggedInUser();
        
        Document document = getDocumentOrThrow(documentId);

        WorkSpaceMember membership = getMembershipOrThrow(user, document.getWorkSpace());

        if(membership.getRole() != WorkSpaceRole.OWNER || membership.getRole() != WorkSpaceRole.ADMIN) {
            throw new InsufficientPermissionException("Only workspace owners or admin can unarchive documents");
        }

        document.setArchived(false);
        documentRepo.save(document);
    }
    
}