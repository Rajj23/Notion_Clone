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
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.enums.WorkSpaceRole;
import com.blockverse.app.exception.DocumentNotFoundException;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.WorkSpaceNotFoundException;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.repo.DocumentRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepo documentRepo;
    @Mock
    private WorkSpaceRepo workSpaceRepo;
    @Mock
    private WorkSpaceMemberRepo workSpaceMemberRepo;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private BlockService blockService;
    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private WorkSpace testWorkSpace;
    private WorkSpaceMember ownerMember;
    private WorkSpaceMember adminMember;
    private WorkSpaceMember regularMember;
    private Document testDocument;
    private DocumentResponse testDocResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1).name("Test User").email("test@mail.com").build();
        testWorkSpace = WorkSpace.builder().id(1).name("Test Workspace").build();
        ownerMember = WorkSpaceMember.builder().id(1).user(testUser).workSpace(testWorkSpace).role(WorkSpaceRole.OWNER)
                .build();
        adminMember = WorkSpaceMember.builder().id(2).user(testUser).workSpace(testWorkSpace).role(WorkSpaceRole.ADMIN)
                .build();
        regularMember = WorkSpaceMember.builder().id(3).user(testUser).workSpace(testWorkSpace)
                .role(WorkSpaceRole.MEMBER).build();
        testDocument = Document.builder().id(1).title("Test Doc").workSpace(testWorkSpace).archived(false).build();
        testDocResponse = DocumentResponse.builder()
                .id(1).title("Test Doc").workspaceId(1).archived(false)
                .createdAt(LocalDateTime.now()).build();
    }

    private void stubAuthenticatedMember(WorkSpaceMember member) {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(testUser, testWorkSpace))
                .thenReturn(Optional.of(member));
    }

    private void stubAuthenticatedNonMember() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(testUser, testWorkSpace))
                .thenReturn(Optional.empty());
    }

    // createDocument

    @Nested
    @DisplayName("createDocument")
    class CreateDocumentTests {

        @Test
        @DisplayName("should create document with correct title and workspace association")
        void createDocument_success() {
            stubAuthenticatedMember(ownerMember);
            when(workSpaceRepo.findById(1)).thenReturn(Optional.of(testWorkSpace));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> {
                Document d = inv.getArgument(0);
                d.setId(10);
                return d;
            });
            when(documentMapper.toResponse(any(Document.class))).thenReturn(testDocResponse);

            CreateDocumentRequest request = new CreateDocumentRequest("New Doc");
            DocumentResponse response = documentService.createDocument(1, request);

            assertNotNull(response);
            verify(documentRepo)
                    .save(argThat(doc -> "New Doc".equals(doc.getTitle()) && doc.getWorkSpace().equals(testWorkSpace)));
        }

        @Test
        @DisplayName("should reject creation when workspace does not exist")
        void createDocument_workspaceNotFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(workSpaceRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(WorkSpaceNotFoundException.class,
                    () -> documentService.createDocument(999, new CreateDocumentRequest("x")));
            verify(documentRepo, never()).save(any());
        }

        @Test
        @DisplayName("should reject creation when user is not a workspace member")
        void createDocument_nonMember() {
            stubAuthenticatedNonMember();
            when(workSpaceRepo.findById(1)).thenReturn(Optional.of(testWorkSpace));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.createDocument(1, new CreateDocumentRequest("x")));
            verify(documentRepo, never()).save(any());
        }
    }

    // getDocument

    @Nested
    @DisplayName("getDocument")
    class GetDocumentTests {

        @Test
        @DisplayName("should return document when user is a workspace member")
        void getDocument_success() {
            stubAuthenticatedMember(ownerMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentMapper.toResponse(testDocument)).thenReturn(testDocResponse);

            DocumentResponse response = documentService.getDocument(1);

            assertNotNull(response);
            assertEquals(1, response.getId());
            assertEquals("Test Doc", response.getTitle());
        }

        @Test
        @DisplayName("should reject when document does not exist")
        void getDocument_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(DocumentNotFoundException.class,
                    () -> documentService.getDocument(999));
        }

        @Test
        @DisplayName("should reject when user is not a workspace member")
        void getDocument_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.getDocument(1));
        }
    }

    // getDocumentWithBlocks

    @Nested
    @DisplayName("getDocumentWithBlocks")
    class GetDocumentWithBlocksTests {

        @Test
        @DisplayName("should return document details with its block tree")
        void getDocumentWithBlocks_success() {
            stubAuthenticatedMember(ownerMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentMapper.toResponse(testDocument)).thenReturn(testDocResponse);

            List<BlockResponse> blocks = List.of(
                    BlockResponse.builder().id(1).documentId(1).type(BlockType.HEADING1)
                            .content("Title").position(BigInteger.valueOf(10000))
                            .children(new ArrayList<>()).build());
            when(blockService.getBlocksForDocument(1)).thenReturn(blocks);

            DocumentDetailsResponse response = documentService.getDocumentWithBlocks(1);

            assertNotNull(response);
            assertNotNull(response.getDocument());
            assertEquals(1, response.getBlocks().size());
            assertEquals("Title", response.getBlocks().get(0).getContent());
        }

        @Test
        @DisplayName("should return document with empty block list when document has no blocks")
        void getDocumentWithBlocks_noBlocks() {
            stubAuthenticatedMember(ownerMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentMapper.toResponse(testDocument)).thenReturn(testDocResponse);
            when(blockService.getBlocksForDocument(1)).thenReturn(List.of());

            DocumentDetailsResponse response = documentService.getDocumentWithBlocks(1);

            assertNotNull(response);
            assertTrue(response.getBlocks().isEmpty());
        }

        @Test
        @DisplayName("should reject when user is not a workspace member")
        void getDocumentWithBlocks_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.getDocumentWithBlocks(1));
        }
    }

    // updateDocument

    @Nested
    @DisplayName("updateDocument")
    class UpdateDocumentTests {

        @Test
        @DisplayName("should update document title and persist the change")
        void updateDocument_success() {
            stubAuthenticatedMember(ownerMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            DocumentResponse updatedResponse = DocumentResponse.builder()
                    .id(1).title("Updated Title").workspaceId(1).archived(false).build();
            when(documentMapper.toResponse(any(Document.class))).thenReturn(updatedResponse);

            UpdateDocumentRequest request = new UpdateDocumentRequest();
            request.setTitle("Updated Title");

            DocumentResponse response = documentService.updateDocument(1, request);

            assertEquals("Updated Title", response.getTitle());
            verify(documentRepo).save(testDocument);
            assertEquals("Updated Title", testDocument.getTitle());
        }

        @Test
        @DisplayName("should reject when document does not exist")
        void updateDocument_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            UpdateDocumentRequest request = new UpdateDocumentRequest();
            request.setTitle("x");

            assertThrows(DocumentNotFoundException.class,
                    () -> documentService.updateDocument(999, request));
        }

        @Test
        @DisplayName("should reject when user is not a workspace member")
        void updateDocument_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            UpdateDocumentRequest request = new UpdateDocumentRequest();
            request.setTitle("x");

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.updateDocument(1, request));
            verify(documentRepo, never()).save(any());
        }
    }

    // getDocumentsByWorkspace

    @Nested
    @DisplayName("getDocumentsByWorkspace")
    class GetDocumentsByWorkspaceTests {

        @Test
        @DisplayName("should return only non-archived documents for the workspace")
        void getDocumentsByWorkspace_success() {
            stubAuthenticatedMember(ownerMember);
            when(workSpaceRepo.findById(1)).thenReturn(Optional.of(testWorkSpace));

            Document doc1 = Document.builder().id(1).title("Doc 1").workSpace(testWorkSpace).build();
            Document doc2 = Document.builder().id(2).title("Doc 2").workSpace(testWorkSpace).build();
            when(documentRepo.findByWorkSpaceAndArchivedFalse(testWorkSpace))
                    .thenReturn(List.of(doc1, doc2));

            DocumentResponse resp1 = DocumentResponse.builder().id(1).title("Doc 1").workspaceId(1).build();
            DocumentResponse resp2 = DocumentResponse.builder().id(2).title("Doc 2").workspaceId(1).build();
            when(documentMapper.toResponse(doc1)).thenReturn(resp1);
            when(documentMapper.toResponse(doc2)).thenReturn(resp2);

            List<DocumentResponse> result = documentService.getDocumentsByWorkspace(1);

            assertEquals(2, result.size());
            assertEquals("Doc 1", result.get(0).getTitle());
            assertEquals("Doc 2", result.get(1).getTitle());
        }

        @Test
        @DisplayName("should return empty list when no non-archived documents exist")
        void getDocumentsByWorkspace_empty() {
            stubAuthenticatedMember(ownerMember);
            when(workSpaceRepo.findById(1)).thenReturn(Optional.of(testWorkSpace));
            when(documentRepo.findByWorkSpaceAndArchivedFalse(testWorkSpace)).thenReturn(List.of());

            List<DocumentResponse> result = documentService.getDocumentsByWorkspace(1);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should reject when workspace does not exist")
        void getDocumentsByWorkspace_workspaceNotFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(workSpaceRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(WorkSpaceNotFoundException.class,
                    () -> documentService.getDocumentsByWorkspace(999));
        }

        @Test
        @DisplayName("should reject when user is not a workspace member")
        void getDocumentsByWorkspace_nonMember() {
            stubAuthenticatedNonMember();
            when(workSpaceRepo.findById(1)).thenReturn(Optional.of(testWorkSpace));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.getDocumentsByWorkspace(1));
        }
    }

    // archiveDocument — OWNER and ADMIN should be allowed, MEMBER should not

    @Nested
    @DisplayName("archiveDocument")
    class ArchiveDocumentTests {

        @Test
        @DisplayName("OWNER should be able to archive a document")
        void archiveDocument_ownerSuccess() {
            stubAuthenticatedMember(ownerMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.archiveDocument(1);

            assertTrue(testDocument.isArchived());
            verify(documentRepo).save(testDocument);
        }

        @Test
        @DisplayName("ADMIN should be able to archive a document")
        void archiveDocument_adminSuccess() {
            stubAuthenticatedMember(adminMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.archiveDocument(1);

            assertTrue(testDocument.isArchived());
            verify(documentRepo).save(testDocument);
        }

        @Test
        @DisplayName("MEMBER should NOT be able to archive — must throw InsufficientPermissionException")
        void archiveDocument_memberDenied() {
            stubAuthenticatedMember(regularMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.archiveDocument(1));
            assertFalse(testDocument.isArchived());
            verify(documentRepo, never()).save(any());
        }

        @Test
        @DisplayName("should reject when document does not exist")
        void archiveDocument_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(DocumentNotFoundException.class,
                    () -> documentService.archiveDocument(999));
        }

        @Test
        @DisplayName("should reject when user is not a workspace member")
        void archiveDocument_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.archiveDocument(1));
            verify(documentRepo, never()).save(any());
        }
    }

    // unarchiveDocument — OWNER and ADMIN should be allowed, MEMBER should not

    @Nested
    @DisplayName("unarchiveDocument")
    class UnarchiveDocumentTests {

        @BeforeEach
        void archiveFirst() {
            testDocument.setArchived(true);
        }

        @Test
        @DisplayName("OWNER should be able to unarchive a document")
        void unarchiveDocument_ownerSuccess() {
            stubAuthenticatedMember(ownerMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.unarchiveDocument(1);

            assertFalse(testDocument.isArchived());
            verify(documentRepo).save(testDocument);
        }

        @Test
        @DisplayName("ADMIN should be able to unarchive a document")
        void unarchiveDocument_adminSuccess() {
            stubAuthenticatedMember(adminMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            documentService.unarchiveDocument(1);

            assertFalse(testDocument.isArchived());
            verify(documentRepo).save(testDocument);
        }

        @Test
        @DisplayName("MEMBER should NOT be able to unarchive — must throw InsufficientPermissionException")
        void unarchiveDocument_memberDenied() {
            stubAuthenticatedMember(regularMember);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.unarchiveDocument(1));
            assertTrue(testDocument.isArchived());
            verify(documentRepo, never()).save(any());
        }

        @Test
        @DisplayName("should reject when document does not exist")
        void unarchiveDocument_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(DocumentNotFoundException.class,
                    () -> documentService.unarchiveDocument(999));
        }

        @Test
        @DisplayName("should reject when user is not a workspace member")
        void unarchiveDocument_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(InsufficientPermissionException.class,
                    () -> documentService.unarchiveDocument(1));
            verify(documentRepo, never()).save(any());
        }
    }
}
