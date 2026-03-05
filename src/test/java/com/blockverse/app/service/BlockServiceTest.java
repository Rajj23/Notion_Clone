package com.blockverse.app.service;

import com.blockverse.app.dto.block.*;
import com.blockverse.app.entity.*;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.exception.*;
import com.blockverse.app.repo.BlockRepo;
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
    private WorkSpaceRepo workSpaceRepo;
    @Mock
    private WorkSpaceMemberRepo workSpaceMemberRepo;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private BlockRepo blockRepo;

    @InjectMocks
    private BlockService blockService;

    private User testUser;
    private WorkSpace testWorkSpace;
    private WorkSpaceMember testMember;
    private Document testDocument;
    private Block testBlock;

    @BeforeEach
    void setUp() {
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
    }

    private void stubAuthenticatedNonMember() {
        when(securityUtil.getLoggedInUser()).thenReturn(testUser);
        when(workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(testUser, testWorkSpace))
                .thenReturn(Optional.empty());
    }

    // ========================================================================
    // createBlock
    // ========================================================================

    @Nested
    @DisplayName("createBlock")
    class CreateBlockTests {

        @Test
        @DisplayName("should create a root block with default position 10000")
        void createRootBlock_success() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));
            when(blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(testDocument, null))
                    .thenReturn(List.of());
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> {
                Block b = inv.getArgument(0);
                b.setId(10);
                return b;
            });

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "New content");
            BlockResponse response = blockService.createBlock(1, request);

            assertNotNull(response);
            assertEquals(BlockType.PARAGRAPH, response.getType());
            assertEquals("New content", response.getContent());
            assertEquals(BigInteger.valueOf(10000), response.getPosition());
            verify(blockRepo).save(any(Block.class));
        }

        @Test
        @DisplayName("should create a child block under a parent")
        void createChildBlock_success() {
            stubAuthenticatedMember();
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

            CreateBlockRequest request = new CreateBlockRequest(5, BlockType.BULLET, "Child");
            BlockResponse response = blockService.createBlock(1, request);

            assertNotNull(response);
            assertEquals(5, response.getParentId());
            assertEquals(BlockType.BULLET, response.getType());
        }

        @Test
        @DisplayName("should calculate position after last sibling")
        void createBlock_positionAfterLastSibling() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Block existingSibling = Block.builder().id(20).document(testDocument)
                    .type(BlockType.PARAGRAPH).content("existing")
                    .position(BigInteger.valueOf(30000)).children(new ArrayList<>()).build();
            when(blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(testDocument, null))
                    .thenReturn(List.of(existingSibling));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "After sibling");
            BlockResponse response = blockService.createBlock(1, request);

            assertEquals(BigInteger.valueOf(40000), response.getPosition());
        }

        @Test
        @DisplayName("should throw BlockLevelException when parent belongs to different document")
        void createBlock_parentFromDifferentDocument() {
            stubAuthenticatedMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            Document otherDoc = Document.builder().id(99).title("Other").workSpace(testWorkSpace).build();
            Block parentInOtherDoc = Block.builder().id(7).document(otherDoc)
                    .type(BlockType.PARAGRAPH).content("x").children(new ArrayList<>()).build();
            when(blockRepo.findById(7)).thenReturn(Optional.of(parentInOtherDoc));

            CreateBlockRequest request = new CreateBlockRequest(7, BlockType.PARAGRAPH, "Bad");

            assertThrows(BlockLevelException.class, () -> blockService.createBlock(1, request));
        }

        @Test
        @DisplayName("should throw DocumentNotFoundException when document does not exist")
        void createBlock_documentNotFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(documentRepo.findById(999)).thenReturn(Optional.empty());

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x");

            assertThrows(DocumentNotFoundException.class, () -> blockService.createBlock(999, request));
        }

        @Test
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a workspace member")
        void createBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(documentRepo.findById(1)).thenReturn(Optional.of(testDocument));

            CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x");

            assertThrows(NotWorkSpaceMemberException.class, () -> blockService.createBlock(1, request));
            verify(blockRepo, never()).save(any());
        }
    }

    // ========================================================================
    // getBlocksForDocument
    // ========================================================================

    @Nested
    @DisplayName("getBlocksForDocument")
    class GetBlocksForDocumentTests {

        @Test
        @DisplayName("should build hierarchical tree from flat block list")
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

            assertEquals(1, result.size());
            assertEquals("Root", result.get(0).getContent());
            assertEquals(1, result.get(0).getChildren().size());
            assertEquals("Child", result.get(0).getChildren().get(0).getContent());
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
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a member")
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
        @DisplayName("should update block content and type")
        void updateBlock_success() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.HEADING2).content("Updated").build();
            BlockResponse response = blockService.updateBlock(1, request);

            assertEquals(BlockType.HEADING2, response.getType());
            assertEquals("Updated", response.getContent());
            verify(blockRepo).save(testBlock);
        }

        @Test
        @DisplayName("should throw BlockNotFoundException when block does not exist")
        void updateBlock_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(blockRepo.findById(999)).thenReturn(Optional.empty());

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.PARAGRAPH).content("x").build();

            assertThrows(BlockNotFoundException.class, () -> blockService.updateBlock(999, request));
        }

        @Test
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a member")
        void updateBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            UpdateBlockRequest request = UpdateBlockRequest.builder()
                    .type(BlockType.PARAGRAPH).content("x").build();

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.updateBlock(1, request));
            verify(blockRepo, never()).save(any());
        }
    }

    // ========================================================================
    // deleteBlock
    // ========================================================================

    @Nested
    @DisplayName("deleteBlock")
    class DeleteBlockTests {

        @Test
        @DisplayName("should soft-delete block by setting deleted flag to true")
        void deleteBlock_success() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            blockService.deleteBlock(1);

            assertTrue(testBlock.isDeleted());
            verify(blockRepo).save(testBlock);
        }

        @Test
        @DisplayName("should throw BlockNotFoundException when block does not exist")
        void deleteBlock_notFound() {
            when(securityUtil.getLoggedInUser()).thenReturn(testUser);
            when(blockRepo.findById(999)).thenReturn(Optional.empty());

            assertThrows(BlockNotFoundException.class, () -> blockService.deleteBlock(999));
        }

        @Test
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a member")
        void deleteBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            assertThrows(NotWorkSpaceMemberException.class, () -> blockService.deleteBlock(1));
            verify(blockRepo, never()).save(any());
        }
    }

    // ========================================================================
    // reorderBlock
    // ========================================================================

    @Nested
    @DisplayName("reorderBlock")
    class ReorderBlockTests {

        @Test
        @DisplayName("should update block position")
        void reorderBlock_success() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            ReorderBlockRequest request = new ReorderBlockRequest();
            request.setNewPosition(BigInteger.valueOf(50000));

            BlockResponse response = blockService.reorderBlock(1, request);

            assertEquals(BigInteger.valueOf(50000), response.getPosition());
            verify(blockRepo).save(testBlock);
        }

        @Test
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a member")
        void reorderBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            ReorderBlockRequest request = new ReorderBlockRequest();
            request.setNewPosition(BigInteger.valueOf(50000));

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.reorderBlock(1, request));
        }
    }

    // ========================================================================
    // getChildren
    // ========================================================================

    @Nested
    @DisplayName("getChildren")
    class GetChildrenTests {

        @Test
        @DisplayName("should return list of child blocks mapped to responses")
        void getChildren_success() {
            stubAuthenticatedMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            Block child1 = Block.builder().id(10).document(testDocument).parent(testBlock)
                    .type(BlockType.BULLET).content("Item 1")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            Block child2 = Block.builder().id(11).document(testDocument).parent(testBlock)
                    .type(BlockType.BULLET).content("Item 2")
                    .position(BigInteger.valueOf(20000)).children(new ArrayList<>()).build();

            when(blockRepo.findByParentAndDeletedFalseOrderByPositionAsc(testBlock))
                    .thenReturn(List.of(child1, child2));

            List<BlockResponse> result = blockService.getChildren(1);

            assertEquals(2, result.size());
            assertEquals("Item 1", result.get(0).getContent());
            assertEquals("Item 2", result.get(1).getContent());
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
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a member")
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
        @DisplayName("should move block to a new parent within the same document")
        void moveBlock_toNewParent() {
            stubAuthenticatedMember();
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
        }

        @Test
        @DisplayName("should move block to root level when newParentId is null")
        void moveBlock_toRoot() {
            stubAuthenticatedMember();
            Block blockWithParent = Block.builder().id(1).document(testDocument)
                    .parent(Block.builder().id(99).document(testDocument).children(new ArrayList<>()).build())
                    .type(BlockType.PARAGRAPH).content("Moving to root")
                    .position(BigInteger.valueOf(10000)).children(new ArrayList<>()).build();
            when(blockRepo.findById(1)).thenReturn(Optional.of(blockWithParent));
            when(blockRepo.save(any(Block.class))).thenAnswer(inv -> inv.getArgument(0));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(500));

            BlockResponse response = blockService.moveBlock(1, request);

            assertNull(response.getParentId());
            assertEquals(BigInteger.valueOf(500), response.getPosition());
        }

        @Test
        @DisplayName("should throw DocumentLevelException when new parent is in a different document")
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
        }

        @Test
        @DisplayName("should throw BlockLevelException when moving block under its own descendant")
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
        }

        @Test
        @DisplayName("should throw NotWorkSpaceMemberException when user is not a member")
        void moveBlock_nonMember() {
            stubAuthenticatedNonMember();
            when(blockRepo.findById(1)).thenReturn(Optional.of(testBlock));

            MoveBlockRequest request = new MoveBlockRequest();
            request.setNewParentId(null);
            request.setNewPosition(BigInteger.valueOf(10000));

            assertThrows(NotWorkSpaceMemberException.class,
                    () -> blockService.moveBlock(1, request));
            verify(blockRepo, never()).save(any());
        }
    }
}
