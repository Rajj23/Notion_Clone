package com.blockverse.app.controller;

import com.blockverse.app.dto.block.*;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.exception.*;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.security.AuthService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.service.BlockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BlockController.class)
@AutoConfigureMockMvc(addFilters = false)
class BlockControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private BlockService blockService;
        @MockitoBean
        private JwtUtil jwtUtil;
        @MockitoBean
        private UserRepo userRepo;
        @MockitoBean
        private UserDetailsService userDetailsService;
        @MockitoBean
        private AuthService authService;

        private BlockResponse sampleBlock;

        @BeforeEach
        void setUp() {
                sampleBlock = BlockResponse.builder()
                                .id(1).documentId(1).parentId(null)
                                .type(BlockType.PARAGRAPH).content("Hello World")
                                .position(BigInteger.valueOf(10000))
                                .children(new ArrayList<>())
                                .build();
        }

        // ========================================================================
        // POST /v1/blocks/{documentId} — createBlock
        // ========================================================================

        @Nested
        @DisplayName("POST /v1/blocks/{documentId}")
        class CreateBlockTests {

                @Test
                @DisplayName("should return 200 with created block response")
                void createBlock_success() throws Exception {
                        when(blockService.createBlock(eq(1), any(CreateBlockRequest.class)))
                                        .thenReturn(sampleBlock);

                        CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "Hello World",
                                        null);

                        mockMvc.perform(post("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.documentId").value(1))
                                        .andExpect(jsonPath("$.type").value("PARAGRAPH"))
                                        .andExpect(jsonPath("$.content").value("Hello World"))
                                        .andExpect(jsonPath("$.position").value(10000))
                                        .andExpect(jsonPath("$.parentId").isEmpty())
                                        .andExpect(jsonPath("$.children").isArray());

                        verify(blockService).createBlock(eq(1), any(CreateBlockRequest.class));
                }

                @Test
                @DisplayName("should return 200 with child block when parentId is provided")
                void createBlock_withParent() throws Exception {
                        BlockResponse childBlock = BlockResponse.builder()
                                        .id(2).documentId(1).parentId(1)
                                        .type(BlockType.BULLET).content("Child")
                                        .position(BigInteger.valueOf(10000))
                                        .children(new ArrayList<>())
                                        .build();
                        when(blockService.createBlock(eq(1), any(CreateBlockRequest.class)))
                                        .thenReturn(childBlock);

                        CreateBlockRequest request = new CreateBlockRequest(1, BlockType.BULLET, "Child", null);

                        mockMvc.perform(post("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.parentId").value(1))
                                        .andExpect(jsonPath("$.type").value("BULLET"));
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void createBlock_documentNotFound() throws Exception {
                        when(blockService.createBlock(eq(999), any(CreateBlockRequest.class)))
                                        .thenThrow(new DocumentNotFoundException("Document not found"));

                        CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x", null);

                        mockMvc.perform(post("/v1/blocks/999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void createBlock_nonMember() throws Exception {
                        when(blockService.createBlock(eq(1), any(CreateBlockRequest.class)))
                                        .thenThrow(new NotWorkSpaceMemberException(
                                                        "User is not a member of this workspace"));

                        CreateBlockRequest request = new CreateBlockRequest(null, BlockType.PARAGRAPH, "x", null);

                        mockMvc.perform(post("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("should return 400 when parent belongs to a different document")
                void createBlock_parentCrossDocument() throws Exception {
                        when(blockService.createBlock(eq(1), any(CreateBlockRequest.class)))
                                        .thenThrow(new BlockLevelException(
                                                        "Parent block must belong to the same document"));

                        CreateBlockRequest request = new CreateBlockRequest(99, BlockType.PARAGRAPH, "x", null);

                        mockMvc.perform(post("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }
        }

        // ========================================================================
        // PUT /v1/blocks/{blockId} — updateBlock
        // ========================================================================

        @Nested
        @DisplayName("PUT /v1/blocks/{blockId}")
        class UpdateBlockTests {

                @Test
                @DisplayName("should return 200 with updated block response")
                void updateBlock_success() throws Exception {
                        BlockResponse updatedBlock = BlockResponse.builder()
                                        .id(1).documentId(1).parentId(null)
                                        .type(BlockType.HEADING2).content("Updated")
                                        .position(BigInteger.valueOf(10000))
                                        .children(new ArrayList<>())
                                        .build();
                        when(blockService.updateBlock(eq(1), any(UpdateBlockRequest.class)))
                                        .thenReturn(updatedBlock);

                        UpdateBlockRequest request = UpdateBlockRequest.builder()
                                        .type(BlockType.HEADING2).content("Updated").build();

                        mockMvc.perform(put("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.type").value("HEADING2"))
                                        .andExpect(jsonPath("$.content").value("Updated"));

                        verify(blockService).updateBlock(eq(1), any(UpdateBlockRequest.class));
                }

                @Test
                @DisplayName("should return 404 when block does not exist")
                void updateBlock_notFound() throws Exception {
                        when(blockService.updateBlock(eq(999), any(UpdateBlockRequest.class)))
                                        .thenThrow(new BlockNotFoundException("Block not found"));

                        UpdateBlockRequest request = UpdateBlockRequest.builder()
                                        .type(BlockType.PARAGRAPH).content("x").build();

                        mockMvc.perform(put("/v1/blocks/999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void updateBlock_nonMember() throws Exception {
                        when(blockService.updateBlock(eq(1), any(UpdateBlockRequest.class)))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        UpdateBlockRequest request = UpdateBlockRequest.builder()
                                        .type(BlockType.PARAGRAPH).content("x").build();

                        mockMvc.perform(put("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // DELETE /v1/blocks/{blockId} — deleteBlock
        // ========================================================================

        @Nested
        @DisplayName("DELETE /v1/blocks/{blockId}")
        class DeleteBlockTests {

                @Test
                @DisplayName("should return 200 with success message")
                void deleteBlock_success() throws Exception {
                        doNothing().when(blockService).deleteBlock(eq(1), any(DeleteBlockRequest.class));

                        DeleteBlockRequest request = new DeleteBlockRequest(null);

                        mockMvc.perform(delete("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Block deleted successfully"));

                        verify(blockService).deleteBlock(eq(1), any(DeleteBlockRequest.class));
                }

                @Test
                @DisplayName("should return 404 when block does not exist")
                void deleteBlock_notFound() throws Exception {
                        doThrow(new BlockNotFoundException("Block not found"))
                                        .when(blockService).deleteBlock(eq(999), any(DeleteBlockRequest.class));

                        DeleteBlockRequest request = new DeleteBlockRequest(null);

                        mockMvc.perform(delete("/v1/blocks/999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void deleteBlock_nonMember() throws Exception {
                        doThrow(new NotWorkSpaceMemberException("User is not a member"))
                                        .when(blockService).deleteBlock(eq(1), any(DeleteBlockRequest.class));

                        DeleteBlockRequest request = new DeleteBlockRequest(null);

                        mockMvc.perform(delete("/v1/blocks/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // PUT /v1/blocks/{blockId}/move — moveBlock
        // ========================================================================

        @Nested
        @DisplayName("PUT /v1/blocks/{blockId}/move")
        class MoveBlockTests {

                @Test
                @DisplayName("should return 200 with moved block response")
                void moveBlock_success() throws Exception {
                        BlockResponse movedBlock = BlockResponse.builder()
                                        .id(1).documentId(1).parentId(50)
                                        .type(BlockType.PARAGRAPH).content("Hello World")
                                        .position(BigInteger.valueOf(20000))
                                        .children(new ArrayList<>())
                                        .build();
                        when(blockService.moveBlock(eq(1), any(MoveBlockRequest.class)))
                                        .thenReturn(movedBlock);

                        MoveBlockRequest request = new MoveBlockRequest();
                        request.setNewParentId(50);
                        request.setNewPosition(BigInteger.valueOf(20000));

                        mockMvc.perform(put("/v1/blocks/1/move")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.parentId").value(50))
                                        .andExpect(jsonPath("$.position").value(20000));

                        verify(blockService).moveBlock(eq(1), any(MoveBlockRequest.class));
                }

                @Test
                @DisplayName("should return 400 when moving to a parent in a different document")
                void moveBlock_crossDocument() throws Exception {
                        when(blockService.moveBlock(eq(1), any(MoveBlockRequest.class)))
                                        .thenThrow(new DocumentLevelException(
                                                        "New parent must belong to the same document"));

                        MoveBlockRequest request = new MoveBlockRequest();
                        request.setNewParentId(60);
                        request.setNewPosition(BigInteger.valueOf(10000));

                        mockMvc.perform(put("/v1/blocks/1/move")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("should return 400 when moving block under its own descendant")
                void moveBlock_descendantCycle() throws Exception {
                        when(blockService.moveBlock(eq(1), any(MoveBlockRequest.class)))
                                        .thenThrow(new BlockLevelException(
                                                        "Cannot move a block under its own descendant"));

                        MoveBlockRequest request = new MoveBlockRequest();
                        request.setNewParentId(3);
                        request.setNewPosition(BigInteger.valueOf(10000));

                        mockMvc.perform(put("/v1/blocks/1/move")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("should return 404 when block does not exist")
                void moveBlock_notFound() throws Exception {
                        when(blockService.moveBlock(eq(999), any(MoveBlockRequest.class)))
                                        .thenThrow(new BlockNotFoundException("Block not found"));

                        MoveBlockRequest request = new MoveBlockRequest();
                        request.setNewParentId(null);
                        request.setNewPosition(BigInteger.valueOf(10000));

                        mockMvc.perform(put("/v1/blocks/999/move")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void moveBlock_nonMember() throws Exception {
                        when(blockService.moveBlock(eq(1), any(MoveBlockRequest.class)))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        MoveBlockRequest request = new MoveBlockRequest();
                        request.setNewParentId(null);
                        request.setNewPosition(BigInteger.valueOf(10000));

                        mockMvc.perform(put("/v1/blocks/1/move")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // GET /v1/blocks/{blockId}/children — getChildren
        // ========================================================================

        @Nested
        @DisplayName("GET /v1/blocks/{blockId}/children")
        class GetChildrenTests {

                @Test
                @DisplayName("should return 200 with ordered list of children")
                void getChildren_success() throws Exception {
                        BlockResponse child1 = BlockResponse.builder()
                                        .id(10).documentId(1).parentId(1)
                                        .type(BlockType.BULLET).content("Item 1")
                                        .position(BigInteger.valueOf(10000))
                                        .children(new ArrayList<>()).build();
                        BlockResponse child2 = BlockResponse.builder()
                                        .id(11).documentId(1).parentId(1)
                                        .type(BlockType.NUMBERED).content("Item 2")
                                        .position(BigInteger.valueOf(20000))
                                        .children(new ArrayList<>()).build();
                        when(blockService.getChildren(1)).thenReturn(List.of(child1, child2));

                        mockMvc.perform(get("/v1/blocks/1/children"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[0].content").value("Item 1"))
                                        .andExpect(jsonPath("$[0].type").value("BULLET"))
                                        .andExpect(jsonPath("$[1].content").value("Item 2"))
                                        .andExpect(jsonPath("$[1].type").value("NUMBERED"));
                }

                @Test
                @DisplayName("should return 200 with empty list when no children exist")
                void getChildren_empty() throws Exception {
                        when(blockService.getChildren(1)).thenReturn(List.of());

                        mockMvc.perform(get("/v1/blocks/1/children"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(0)));
                }

                @Test
                @DisplayName("should return 404 when parent block does not exist")
                void getChildren_notFound() throws Exception {
                        when(blockService.getChildren(999))
                                        .thenThrow(new BlockNotFoundException("Block not found"));

                        mockMvc.perform(get("/v1/blocks/999/children"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void getChildren_nonMember() throws Exception {
                        when(blockService.getChildren(1))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(get("/v1/blocks/1/children"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // POST /v1/blocks/restore/{documentId} — restoreDocumentVersion
        // ========================================================================

        @Nested
        @DisplayName("POST /v1/blocks/restore/{documentId}")
        class RestoreDocumentVersionTests {

                @Test
                @DisplayName("should return 200 with success message when version is restored")
                void restoreVersion_success() throws Exception {
                        doNothing().when(blockService).restoreDocumentVersion(eq(1),
                                        any(RestoreDocumentVersionRequest.class));

                        mockMvc.perform(post("/v1/blocks/restore/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"targetVersion": 3}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document version restored successfully"));

                        verify(blockService).restoreDocumentVersion(eq(1), any(RestoreDocumentVersionRequest.class));
                }

                @Test
                @DisplayName("should return 400 when target version is invalid")
                void restoreVersion_invalidTarget() throws Exception {
                        doThrow(new DocumentLevelException(
                                        "Target version must be less than or equal to current version"))
                                        .when(blockService)
                                        .restoreDocumentVersion(eq(1), any(RestoreDocumentVersionRequest.class));

                        mockMvc.perform(post("/v1/blocks/restore/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"targetVersion": 999}
                                                        """))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void restoreVersion_notFound() throws Exception {
                        doThrow(new DocumentNotFoundException("Document not found"))
                                        .when(blockService)
                                        .restoreDocumentVersion(eq(999), any(RestoreDocumentVersionRequest.class));

                        mockMvc.perform(post("/v1/blocks/restore/999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"targetVersion": 1}
                                                        """))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void restoreVersion_nonMember() throws Exception {
                        doThrow(new NotWorkSpaceMemberException("User is not a member"))
                                        .when(blockService)
                                        .restoreDocumentVersion(eq(1), any(RestoreDocumentVersionRequest.class));

                        mockMvc.perform(post("/v1/blocks/restore/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"targetVersion": 1}
                                                        """))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // GET /v1/blocks/history/{documentId} — getDocumentHistory
        // ========================================================================

        @Nested
        @DisplayName("GET /v1/blocks/history/{documentId}")
        class GetDocumentHistoryTests {

                @Test
                @DisplayName("should return 200 with list of change logs")
                void getHistory_success() throws Exception {
                        when(blockService.getDocumentHistory(1))
                                        .thenReturn(List.of());

                        mockMvc.perform(get("/v1/blocks/history/1"))
                                        .andExpect(status().isOk());

                        verify(blockService).getDocumentHistory(1);
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void getHistory_notFound() throws Exception {
                        when(blockService.getDocumentHistory(999))
                                        .thenThrow(new DocumentNotFoundException("Document not found"));

                        mockMvc.perform(get("/v1/blocks/history/999"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void getHistory_nonMember() throws Exception {
                        when(blockService.getDocumentHistory(1))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(get("/v1/blocks/history/1"))
                                        .andExpect(status().isForbidden());
                }
        }
}
