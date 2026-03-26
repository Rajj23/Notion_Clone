package com.blockverse.app.service;

import com.blockverse.app.dto.block.*;
import com.blockverse.app.entity.*;
import com.blockverse.app.enums.BlockOperationType;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.enums.AuditActionType;
import com.blockverse.app.enums.AuditEntityType;
import com.blockverse.app.exception.*;
import com.blockverse.app.repo.BlockChangeLogRepo;
import com.blockverse.app.repo.BlockRepo;
import com.blockverse.app.repo.DocumentRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private DocumentRepo documentRepo;
    @Mock
    private WorkSpaceMemberRepo workSpaceMemberRepo;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private BlockRepo blockRepo;
    @Mock
    private BlockChangeLogRepo blockChangeLogRepo;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private S3Service s3Service;
    @Mock
    private DocumentSocketPublisher documentSocketPublisher;
    @Mock
    private RateLimiterService rateLimiterService;

    private com.blockverse.app.mapper.BlockMapper blockMapper;

    private BlockService blockService;

    private User testUser;
    private WorkSpace testWorkSpace;
    private WorkSpaceMember testMember;
    private Document testDocument;
    private Block testBlock;

    @BeforeEach
    void setUp() {
        documentSocketPublisher = mock(DocumentSocketPublisher.class);
        s3Service = mock(S3Service.class);
        blockMapper = new com.blockverse.app.mapper.BlockMapper(s3Service);
        blockService = new BlockService(documentRepo, workSpaceMemberRepo, securityUtil, blockRepo, blockChangeLogRepo, auditLogService, documentSocketPublisher, blockMapper, rateLimiterService);
        testUser = User.builder().id(1).name("Test User").email("test@mail.com").build();
        testWorkSpace = WorkSpace.builder().id(1).name("Test Workspace").build();
        testMember = WorkSpaceMember.builder().id(1).user(testUser).workSpace(testWorkSpace).build();
        testDocument = Document.builder().id(1).title("Test Doc").workSpace(testWorkSpace).build();
        testBlock = Block.builder()
                .id(1)
                .document(testDocument)
                .type(BlockType.PARAGRAPH)
                .content("Hello World")
                .position(BigInteger.valueOf(10000))
                .deleted(false)
                .children(new ArrayList<>())
                .build();
    }

    private void stubAuthenticatedMember() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(testUser, testWorkSpace))
                .thenReturn(Optional.of(testMember));
        lenient().doNothing().when(rateLimiterService).checkRateLimit(anyInt(), anyString());
    }

    private void stubAuthenticatedNonMember() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(testUser, testWorkSpace))
                .thenReturn(Optional.empty());
        lenient().doNothing().when(rateLimiterService).checkRateLimit(anyInt(), anyString());
    }

    private void stubChangeLogDependencies() {
        when(documentRepo.saveAndFlush(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        when(blockChangeLogRepo.save(any(BlockChangeLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubRateLimitExceeded(String action) {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        doThrow(new TooManyRequestsException("Too many requests"))
                .when(rateLimiterService).checkRateLimit(testUser.getId(), action);
    }

    // ========================================================================
    // createBlock
    // ========================================================================

    @Nested
    @DisplayName("createBlock")
    class CreateBlockTests {

        @Test
        @DisplayName("should create a root block with default position 10000 and correct document association")
        void createRootBlock_success() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(testDocument, null))
                    .thenReturn(List.of());
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> {
                Block b = inv.getArgument(0);
                b.setId(10);
                return b;
            });

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "New content", null);
            BlockResponse response = blockService.createBlock(1, request);

            assertNotNull(response);
            assertEquals(BlockType.PARAGRAPH, response.getType());
            assertEquals("New content", response.getContent());
            assertEquals(BigInteger.valueOf(10000), response.getPosition());
            assertNull(response.getParentId());
            assertEquals(1, response.getDocumentId());
            verify(blockRepo).save(argThat(block -> block.getDocument().equals(testDocument) &&
                    block.getParent() == null &&
                    "New content".equals(block.getContent()) &&
                    block.getType() == BlockType.PARAGRAPH &&
                    block.getPosition().equals(BigInteger.valueOf(10000))));
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.BLOCK), eq(10), eq(AuditActionType.BLOCK_CREATED), anyString());
        }

        @Test
        @DisplayName("should create a child block linked to its parent within the same document")
        void createChildBlock_success() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block parent = Block.builder().id(5).document(testDocument)
                    .type(BlockType.HEADING1).content("Parent")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            when(blockRepo.findById(5)).thenReturn(Optional.of(parent));
            when(blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(testDocument, parent))
                    .thenReturn(List.of());
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> {
                Block b = inv.getArgument(0);
                b.setId(11);
                return b;
            });

            CreateBlockRequest request = new CreateBlockRequest(5, BlockType.BULLET, "Child", null);
            BlockResponse response = blockService.createBlock(1, request);

            assertNotNull(response);
            assertEquals(5, response.getParentId());
            assertEquals(BlockType.BULLET, response.getType());
            assertEquals("Child", response.getContent());
            verify(blockRepo).save(argThat(block -> block.getParent() != null &&
                    block.getParent().getId() == 5 &&
                    block.getDocument().equals(testDocument)));
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.BLOCK), eq(11), eq(AuditActionType.BLOCK_CREATED), anyString());
        }

        @Test
        @DisplayName("should calculate position as lastSibling + 10000")
        void createBlock_positionAfterLastSibling() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block existingSibling = Block.builder().id(20).document(testDocument)
                    .type(BlockType.PARAGRAPH).content("existing")
                    .position(BigInteger.valueOf(30000)).children(new ArrayList<>()).build();
            when(blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(testDocument, null))
                    .thenReturn(List.of(existingSibling));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "After sibling", null);
            BlockResponse response = blockService.createBlock(1, request);

            assertEquals(BigInteger.valueOf(40000), response.getPosition());
            verify(blockRepo).save(argThat(block -> block.getPosition().equals(BigInteger.valueOf(40000))));
        }

        @Test
        @DisplayName("must reject parent block from a different document")
        void createBlock_parentFromDifferentDocument() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Document otherDoc = Document.builder().id(99).title("Other").workSpace(testWorkSpace).build();
            Block parentInOtherDoc = Block.builder().id(7).document(otherDoc)
                    .type(BlockType.PARAGRAPH).content("x").children(new ArrayList<>()).build();
            when(blockRepo.findById(7)).thenReturn(Optional.of(parentInOtherDoc));

            CreateBlockRequest request = new CreateBlockRequest(7, BlockType.PARAGRAPH, "Bad", null);

            assertThrows(BlockLevelException.class, () -> blockService.createBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("must reject when document does not exist")
        void createBlock_documentNotFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x", null);

            assertThrows(DocumentNotFoundException.class, () -> blockService.createBlock(999, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("must reject non-workspace-member — no data should be saved")
        void createBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x", null);

            assertThrows(NotWorkSpaceMemberException.class, () -> blockService.createBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("must reject when client document version is stale — conflict detected")
        void createBlock_versionConflict() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x", 3L);

            assertThrows(DocumentVersionMismatchException.class, () -> blockService.createBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("should create block when client version matches server version exactly")
        void createBlock_correctVersion() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            testDocument.setVersion(3L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(testDocument, null))
                    .thenReturn(List.of());
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> {
                Block b = inv.getArgument(0);
                b.setId(10);
                return b;
            });

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "Versioned", 3L);
            BlockResponse response = blockService.createBlock(1, request);

            assertNotNull(response);
            assertEquals("Versioned", response.getContent());
            verify(blockRepo).save(any(Block.class));
        }
    }

    // ========================================================================
    // getBlocksForDocument
    // ========================================================================

    @Nested
    @DisplayName("getBlocksForDocument")
    class GetBlocksForDocumentTests {

        @Test
        @DisplayName("should build correct hierarchical tree — children nested under parents")
        void getBlocks_buildsTree() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block root = Block.builder().id(1).document(testDocument).parent(null)
                    .type(BlockType.HEADING1).content("Root")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            Block child = Block.builder().id(2).document(testDocument).parent(root)
                    .type(BlockType.PARAGRAPH).content("Child")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();

            when(blockRepo.findByDocumentAndDeletedFalseOrderByPositionAsc(testDocument))
                    .thenReturn(List.of(root, child));

            List<BlockResponse> result = blockService.getBlocksForDocument(1);

            assertEquals(1, result.size(), "Only root blocks should be at top level");
            assertEquals("Root", result.getFirst().getContent());
            assertEquals(BlockType.HEADING1, result.getFirst().getType());
            assertEquals(1, result.getFirst().getChildren().size(), "Root should have 1 child");
            assertEquals("Child", result.getFirst().getChildren().getFirst().getContent());
            assertNull(result.getFirst().getParentId(), "Root block should have null parent");
        }

        @Test
        @DisplayName("should return empty list when document has no blocks")
        void getBlocks_emptyDocument() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(blockRepo.findByDocumentAndDeletedFalseOrderByPositionAsc(testDocument))
                    .thenReturn(List.of());

            List<BlockResponse> result = blockService.getBlocksForDocument(1);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("must reject non-workspace-member")
        void getBlocks_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.getBlocksForDocument(1));
        }
    }

    // ========================================================================
    // updateBlock
    // ========================================================================

    @Nested
    @DisplayName("updateBlock")
    class UpdateBlockTests {

        @Test
        @DisplayName("should mutate block content and type on the entity, then persist")
        void updateBlock_success() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.HEADING2).content("Updated").build();
            BlockResponse response = blockService.updateBlock(1, request);

            assertEquals(BlockType.HEADING2, response.getType());
            assertEquals("Updated", response.getContent());

            assertEquals(BlockType.HEADING2, testBlock.getType(), "Entity type should be mutated");
            assertEquals("Updated", testBlock.getContent(), "Entity content should be mutated");
            verify(blockRepo).save(testBlock);
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.BLOCK), eq(1), eq(AuditActionType.BLOCK_UPDATED), anyString());
        }

        @Test
        @DisplayName("must reject when block does not exist")
        void updateBlock_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(blockRepo.findById(999)).thenReturn(Optional.empty());

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.PARAGRAPH).content("x").build();

            assertThrows(BlockNotFoundException.class, () -> blockService.updateBlock(999, request));
        }

        @Test
        @DisplayName("must reject non-workspace-member — no data should be saved")
        void updateBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.PARAGRAPH).content("x").build();

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.updateBlock(1, request));
            verify(blockRepo, never()).save(any());

            assertEquals("Hello World", testBlock.getContent(), "Entity should not be modified by non-member");
        }

        @Test
        @DisplayName("must reject when client document version is stale — conflict detected")
        void updateBlock_versionConflict() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.PARAGRAPH).content("x").documentVersion(3L).build();

            assertThrows(DocumentVersionMismatchException.class,
                    () -> blockService.updateBlock(1, request));
            verify(blockRepo, never()).save(any());
            assertEquals("Hello World", testBlock.getContent(), "Entity should not be modified on version conflict");
        }

        @Test
        @DisplayName("should update when client version matches server version exactly")
        void updateBlock_correctVersion() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            testDocument.setVersion(3L);
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.HEADING2).content("Updated").documentVersion(3L).build();
            BlockResponse response = blockService.updateBlock(1, request);

            assertEquals("Updated", response.getContent());
            verify(blockRepo).save(testBlock);
        }
    }

    // ========================================================================
    // deleteBlock
    // ========================================================================

    @Nested
    @DisplayName("deleteBlock")
    class DeleteBlockTests {

        @Test
        @DisplayName("should soft-delete by setting deleted=true on the entity and persisting")
        void deleteBlock_success() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            assertFalse(testBlock.isDeleted(), "Block should not be deleted before operation");

            DeleteBlockRequest request = new DeleteBlockRequest(null);
            blockService.deleteBlock(1, request);

            assertTrue(testBlock.isDeleted(), "Block entity must have deleted=true after soft delete");
            verify(blockRepo).save(testBlock);
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.BLOCK), eq(1), eq(AuditActionType.BLOCK_DELETED), anyString());
        }

        @Test
        @DisplayName("must reject when block does not exist")
        void deleteBlock_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(blockRepo.findById(999)).thenReturn(Optional.empty());

            DeleteBlockRequest request = new DeleteBlockRequest(null);
            assertThrows(BlockNotFoundException.class, () -> blockService.deleteBlock(999, request));
        }

        @Test
        @DisplayName("must reject non-workspace-member — block should remain undeleted")
        void deleteBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            DeleteBlockRequest request = new DeleteBlockRequest(null);
            assertThrows(NotWorkSpaceMemberException.class, () -> blockService.deleteBlock(1, request));
            verify(blockRepo, never()).save(any());
            assertFalse(testBlock.isDeleted(), "Block must remain undeleted when non-member attempts deletion");
        }

        @Test
        @DisplayName("must reject when client document version is stale — conflict detected")
        void deleteBlock_versionConflict() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            DeleteBlockRequest request = new DeleteBlockRequest(3L);
            assertThrows(DocumentVersionMismatchException.class,
                    () -> blockService.deleteBlock(1, request));
            verify(blockRepo, never()).save(any());
            assertFalse(testBlock.isDeleted(), "Block must remain undeleted on version conflict");
        }

        @Test
        @DisplayName("should delete when client version matches server version exactly")
        void deleteBlock_correctVersion() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            testDocument.setVersion(3L);
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            DeleteBlockRequest request = new DeleteBlockRequest(3L);
            blockService.deleteBlock(1, request);

            assertTrue(testBlock.isDeleted(), "Block should be soft-deleted when version matches");
            verify(blockRepo).save(testBlock);
        }
    }

    // ========================================================================
    // getChildren
    // ========================================================================

    @Nested
    @DisplayName("getChildren")
    class GetChildrenTests {

        @Test
        @DisplayName("should return ordered child list with correct content and types")
        void getChildren_success() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            Block child1 = Block.builder().id(10).document(testDocument).parent(testBlock)
                    .type(BlockType.BULLET).content("Item 1")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            Block child2 = Block.builder().id(11).document(testDocument).parent(testBlock)
                    .type(BlockType.NUMBERED).content("Item 2")
                    .position(BigInteger.valueOf(20000)).children(new ArrayList<>()).build();

            when(blockRepo.findByParentAndDeletedFalseOrderByPositionAsc(testBlock))
                    .thenReturn(List.of(child1, child2));

            List<BlockResponse> result = blockService.getChildren(1);

            assertEquals(2, result.size());
            assertEquals("Item 1", result.get(0).getContent());
            assertEquals(BlockType.BULLET, result.get(0).getType());
            assertEquals("Item 2", result.get(1).getContent());
            assertEquals(BlockType.NUMBERED, result.get(1).getType());
        }

        @Test
        @DisplayName("should return empty list when block has no children")
        void getChildren_noChildren() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.findByParentAndDeletedFalseOrderByPositionAsc(testBlock))
                    .thenReturn(List.of());

            List<BlockResponse> result = blockService.getChildren(1);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("must reject non-workspace-member")
        void getChildren_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.getChildren(1));
        }
    }

    // ========================================================================
    // moveBlock
    // ========================================================================

    @Nested
    @DisplayName("moveBlock")
    class MoveBlockTests {

        @Test
        @DisplayName("should move block to new parent — entity must have updated parent and position")
        void moveBlock_toNewParent() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            Block newParent = Block.builder().id(50).document(testDocument)
                    .type(BlockType.HEADING1).content("New Parent")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            when(blockRepo.findById(50)).thenReturn(Optional.of(newParent));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(50);
            request.setNewPosition(BigInteger.valueOf(20000));

            BlockResponse response = blockService.moveBlock(1, request);

            assertEquals(50, response.getParentId());
            assertEquals(BigInteger.valueOf(20000), response.getPosition());

            assertEquals(newParent, testBlock.getParent(), "Entity parent must be updated");
            assertEquals(BigInteger.valueOf(20000), testBlock.getPosition(), "Entity position must be updated");
            verify(blockRepo).save(testBlock);
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.BLOCK), eq(1), eq(AuditActionType.BLOCK_MOVED), anyString());
        }

        @Test
        @DisplayName("should move block to root level — parent set to null")
        void moveBlock_toRoot() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            Block blockWithParent = Block.builder().id(1).document(testDocument)
                    .parent(Block.builder().id(99).document(testDocument).children(new ArrayList<>()).build())
                    .type(BlockType.PARAGRAPH).content("Moving to root")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            when(blockRepo.findById(1)).thenReturn(Optional.of(blockWithParent));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            assertNotNull(blockWithParent.getParent(), "Block should have a parent before move");

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(500));

            BlockResponse response = blockService.moveBlock(1, request);

            assertNull(response.getParentId());
            assertEquals(BigInteger.valueOf(500), response.getPosition());
            assertNull(blockWithParent.getParent(), "Entity parent must be null after moving to root");
        }

        @Test
        @DisplayName("must reject moving block to a parent in a different document")
        void moveBlock_crossDocument() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            Document otherDoc = Document.builder().id(99).title("Other").workSpace(testWorkSpace).build();
            Block foreignParent = Block.builder().id(60).document(otherDoc)
                    .type(BlockType.PARAGRAPH).content("Foreign")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            when(blockRepo.findById(60)).thenReturn(Optional.of(foreignParent));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(60);
            request.setNewPosition(BigInteger.valueOf(10000));

            assertThrows(DocumentLevelException.class, () -> blockService.moveBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("must reject moving block under its own descendant — prevents cycle")
        void moveBlock_descendantCycle() {
            stubAuthenticatedMember();

            Block grandParent = Block.builder().id(1).document(testDocument)
                    .parent(null).type(BlockType.HEADING1).content("GrandParent")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            Block child = Block.builder().id(2).document(testDocument)
                    .parent(grandParent).type(BlockType.PARAGRAPH).content("Child")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            Block grandChild = Block.builder().id(3).document(testDocument)
                    .parent(child).type(BlockType.PARAGRAPH).content("GrandChild")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();

            when(blockRepo.findById(1)).thenReturn(Optional.of(grandParent));
            when(blockRepo.findById(3)).thenReturn(Optional.of(grandChild));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(3);
            request.setNewPosition(BigInteger.valueOf(10000));

            assertThrows(BlockLevelException.class, () -> blockService.moveBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("must reject non-workspace-member — no data should be saved")
        void moveBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            Block originalParent = testBlock.getParent();
            BigInteger originalPos = testBlock.getPosition();

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(10000));

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.moveBlock(1, request));
            verify(blockRepo, never()).save(any());
            assertEquals(originalParent, testBlock.getParent(), "Parent must not change for non-member");
            assertEquals(originalPos, testBlock.getPosition(), "Position must not change for non-member");
        }

        @Test
        @DisplayName("must reject when client document version is stale — conflict detected")
        void moveBlock_versionConflict() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(20000));
            request.setDocumentVersion(3L);

            assertThrows(DocumentVersionMismatchException.class,
                    () -> blockService.moveBlock(1, request));
            verify(blockRepo, never()).save(any());
            assertEquals(BigInteger.valueOf(10000), testBlock.getPosition(),
                    "Position must not change on version conflict");
        }

        @Test
        @DisplayName("should move when client version matches server version exactly")
        void moveBlock_correctVersion() {
            stubAuthenticatedMember();
            stubChangeLogDependencies();
            testDocument.setVersion(3L);
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(20000));
            request.setDocumentVersion(3L);

            BlockResponse response = blockService.moveBlock(1, request);

            assertEquals(BigInteger.valueOf(20000), response.getPosition());
            verify(blockRepo).save(testBlock);
        }
    }

    // ========================================================================
    // restoreDocumentVersion
    // ========================================================================

    @Nested
    @DisplayName("restoreDocumentVersion")
    class RestoreDocumentVersionTests {

        @Test
        @DisplayName("should reverse CREATE operation — sets block deleted=true")
        void restoreVersion_reverseCreate() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block createdBlock = Block.builder().id(10).document(testDocument)
                    .type(BlockType.PARAGRAPH).content("Created")
                    .position(BigInteger.valueOf(10000)).deleted(false)
                    .children(new ArrayList<>()).build();

            BlockChangeLog log = BlockChangeLog.builder()
                    .id(1).document(testDocument).block(createdBlock)
                    .operationType(BlockOperationType.CREATE)
                    .newContent("Created").newPosition(BigInteger.valueOf(10000))
                    .versionNumber(5L).build();

            when(blockChangeLogRepo.findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                    testDocument, 3L)).thenReturn(List.of(log));
            when(blockRepo.findById(10)).thenReturn(Optional.of(createdBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(3L);
            blockService.restoreDocumentVersion(1, request);

            assertTrue(createdBlock.isDeleted(), "Created block should be deleted when version is reversed");
            verify(blockRepo).save(createdBlock);
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.DOCUMENT), eq(1), eq(AuditActionType.DOCUMENT_RESTORED), anyString());
        }

        @Test
        @DisplayName("should reverse DELETE operation — restores block content and position")
        void restoreVersion_reverseDelete() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block deletedBlock = Block.builder().id(10).document(testDocument)
                    .type(BlockType.PARAGRAPH).content(null)
                    .position(null).deleted(true)
                    .children(new ArrayList<>()).build();

            BlockChangeLog log = BlockChangeLog.builder()
                    .id(1).document(testDocument).block(deletedBlock)
                    .operationType(BlockOperationType.DELETE)
                    .oldContent("Original content").oldPosition(BigInteger.valueOf(10000))
                    .versionNumber(5L).build();

            when(blockChangeLogRepo.findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                    testDocument, 3L)).thenReturn(List.of(log));
            when(blockRepo.findById(10)).thenReturn(Optional.of(deletedBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(3L);
            blockService.restoreDocumentVersion(1, request);

            assertFalse(deletedBlock.isDeleted(), "Deleted block should be restored");
            assertEquals("Original content", deletedBlock.getContent());
            assertEquals(BigInteger.valueOf(10000), deletedBlock.getPosition());
        }

        @Test
        @DisplayName("should reverse UPDATE operation — restores old content")
        void restoreVersion_reverseUpdate() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block updatedBlock = Block.builder().id(10).document(testDocument)
                    .type(BlockType.PARAGRAPH).content("New content")
                    .position(BigInteger.valueOf(10000)).deleted(false)
                    .children(new ArrayList<>()).build();

            BlockChangeLog log = BlockChangeLog.builder()
                    .id(1).document(testDocument).block(updatedBlock)
                    .operationType(BlockOperationType.UPDATE)
                    .oldContent("Old content").newContent("New content")
                    .versionNumber(5L).build();

            when(blockChangeLogRepo.findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                    testDocument, 3L)).thenReturn(List.of(log));
            when(blockRepo.findById(10)).thenReturn(Optional.of(updatedBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(3L);
            blockService.restoreDocumentVersion(1, request);

            assertEquals("Old content", updatedBlock.getContent(), "Content should be restored to old value");
        }

        @Test
        @DisplayName("should reverse MOVE operation — restores old parent and position")
        void restoreVersion_reverseMove() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block parentBlock = Block.builder().id(20).document(testDocument)
                    .type(BlockType.HEADING1).content("Parent")
                    .position(BigInteger.valueOf(10000)).deleted(false)
                    .children(new ArrayList<>()).build();

            Block movedBlock = Block.builder().id(10).document(testDocument)
                    .type(BlockType.PARAGRAPH).content("Moved")
                    .position(BigInteger.valueOf(30000)).deleted(false).parent(null)
                    .children(new ArrayList<>()).build();

            BlockChangeLog log = BlockChangeLog.builder()
                    .id(1).document(testDocument).block(movedBlock)
                    .operationType(BlockOperationType.MOVE)
                    .oldPosition(BigInteger.valueOf(10000))
                    .newPosition(BigInteger.valueOf(30000))
                    .oldParentId(20).newParentId(null)
                    .versionNumber(5L).build();

            when(blockChangeLogRepo.findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                    testDocument, 3L)).thenReturn(List.of(log));
            when(blockRepo.findById(10)).thenReturn(Optional.of(movedBlock));
            when(blockRepo.findById(20)).thenReturn(Optional.of(parentBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(3L);
            blockService.restoreDocumentVersion(1, request);

            assertEquals(parentBlock, movedBlock.getParent(), "Parent should be restored");
            assertEquals(BigInteger.valueOf(10000), movedBlock.getPosition(), "Position should be restored");
        }

        @Test
        @DisplayName("should reverse MOVE to root — sets parent to null")
        void restoreVersion_reverseMoveToRoot() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block parentBlock = Block.builder().id(20).document(testDocument)
                    .type(BlockType.HEADING1).content("Parent")
                    .children(new ArrayList<>()).build();

            Block movedBlock = Block.builder().id(10).document(testDocument)
                    .type(BlockType.PARAGRAPH).content("Moved")
                    .parent(parentBlock).position(BigInteger.valueOf(30000)).deleted(false)
                    .children(new ArrayList<>()).build();

            BlockChangeLog log = BlockChangeLog.builder()
                    .id(1).document(testDocument).block(movedBlock)
                    .operationType(BlockOperationType.MOVE)
                    .oldPosition(BigInteger.valueOf(10000))
                    .newPosition(BigInteger.valueOf(30000))
                    .oldParentId(null).newParentId(20)
                    .versionNumber(5L).build();

            when(blockChangeLogRepo.findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                    testDocument, 3L)).thenReturn(List.of(log));
            when(blockRepo.findById(10)).thenReturn(Optional.of(movedBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(3L);
            blockService.restoreDocumentVersion(1, request);

            assertNull(movedBlock.getParent(), "Parent should be null when original had no parent");
            assertEquals(BigInteger.valueOf(10000), movedBlock.getPosition());
        }

        @Test
        @DisplayName("must reject target version greater than current version")
        void restoreVersion_targetVersionTooHigh() {
            stubAuthenticatedMember();
            testDocument.setVersion(3L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(5L);

            assertThrows(DocumentLevelException.class,
                    () -> blockService.restoreDocumentVersion(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("must reject non-workspace-member")
        void restoreVersion_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(1L);

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.restoreDocumentVersion(1, request));
        }

        @Test
        @DisplayName("must reject when document does not exist")
        void restoreVersion_documentNotFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(1L);

            assertThrows(DocumentNotFoundException.class,
                    () -> blockService.restoreDocumentVersion(999, request));
        }

        @Test
        @DisplayName("should skip blocks that no longer exist in the database")
        void restoreVersion_skipsDeletedBlocks() {
            stubAuthenticatedMember();
            testDocument.setVersion(5L);
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block phantomBlock = Block.builder().id(99).build();
            BlockChangeLog log = BlockChangeLog.builder()
                    .id(1).document(testDocument).block(phantomBlock)
                    .operationType(BlockOperationType.CREATE)
                    .versionNumber(5L).build();

            when(blockChangeLogRepo.findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                    testDocument, 3L)).thenReturn(List.of(log));
            when(blockRepo.findById(99)).thenReturn(Optional.empty());
            when(documentRepo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(3L);
            blockService.restoreDocumentVersion(1, request);

            verify(blockRepo, never()).save(any());
            verify(auditLogService).auditLog(eq(1), eq(1), eq(AuditEntityType.DOCUMENT), eq(1), eq(AuditActionType.DOCUMENT_RESTORED), anyString());
        }
    }

    // ========================================================================
    // getDocumentHistory
    // ========================================================================

    @Nested
    @DisplayName("getDocumentHistory")
    class GetDocumentHistoryTests {

        @Test
        @DisplayName("should return change logs for document ordered by version desc")
        void getDocumentHistory_success() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            BlockChangeLog log1 = BlockChangeLog.builder().id(1).document(testDocument)
                    .operationType(BlockOperationType.CREATE).versionNumber(2L).build();
            BlockChangeLog log2 = BlockChangeLog.builder().id(2).document(testDocument)
                    .operationType(BlockOperationType.UPDATE).versionNumber(1L).build();

            when(blockChangeLogRepo.findByDocumentOrderByVersionNumberDesc(testDocument))
                    .thenReturn(List.of(log1, log2));

            List<BlockChangeLogResponse> result = blockService.getDocumentHistory(1);

            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getVersionNumber());
            assertEquals(1L, result.get(1).getVersionNumber());
        }

        @Test
        @DisplayName("should return empty list when no history exists")
        void getDocumentHistory_empty() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(blockChangeLogRepo.findByDocumentOrderByVersionNumberDesc(testDocument))
                    .thenReturn(List.of());

            List<BlockChangeLogResponse> result = blockService.getDocumentHistory(1);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("must reject non-workspace-member")
        void getDocumentHistory_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.getDocumentHistory(1));
        }

        @Test
        @DisplayName("must reject when document does not exist")
        void getDocumentHistory_documentNotFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(DocumentNotFoundException.class,
                    () -> blockService.getDocumentHistory(999));
        }
    }

    // ========================================================================
    // Rate Limiting
    // ========================================================================

    @Nested
    @DisplayName("rateLimiting")
    class RateLimitingTests {

        @Test
        @DisplayName("createBlock — must throw TooManyRequestsException when rate limit exceeded")
        void createBlock_rateLimitExceeded() {
            stubRateLimitExceeded("CREATE_BLOCK");

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x", null);
            assertThrows(TooManyRequestsException.class, () -> blockService.createBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("updateBlock — must throw TooManyRequestsException when rate limit exceeded")
        void updateBlock_rateLimitExceeded() {
            stubRateLimitExceeded("UPDATE_BLOCK");

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.PARAGRAPH).content("x").build();
            assertThrows(TooManyRequestsException.class, () -> blockService.updateBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("deleteBlock — must throw TooManyRequestsException when rate limit exceeded")
        void deleteBlock_rateLimitExceeded() {
            stubRateLimitExceeded("DELETE_BLOCK");

            DeleteBlockRequest request = new DeleteBlockRequest(null);
            assertThrows(TooManyRequestsException.class, () -> blockService.deleteBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("moveBlock — must throw TooManyRequestsException when rate limit exceeded")
        void moveBlock_rateLimitExceeded() {
            stubRateLimitExceeded("MOVE_BLOCK");

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(10000));
            assertThrows(TooManyRequestsException.class, () -> blockService.moveBlock(1, request));
            verify(blockRepo, never()).save(any());
        }

        @Test
        @DisplayName("restoreDocumentVersion — must throw TooManyRequestsException when rate limit exceeded")
        void restoreDocumentVersion_rateLimitExceeded() {
            stubRateLimitExceeded("RESTORE_DOCUMENT");

            RestoreDocumentVersionRequest request = new RestoreDocumentVersionRequest(1L);
            assertThrows(TooManyRequestsException.class, () -> blockService.restoreDocumentVersion(1, request));
            verify(blockRepo, never()).save(any());
        }
    }
}
