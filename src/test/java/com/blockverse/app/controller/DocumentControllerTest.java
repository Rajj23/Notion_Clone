package com.blockverse.app.controller;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.CreateDocumentRequest;
import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.dto.document.UpdateDocumentRequest;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.exception.DocumentException;
import com.blockverse.app.exception.DocumentNotFoundException;
import com.blockverse.app.exception.InsufficientPermissionException;
import com.blockverse.app.exception.NotWorkSpaceMemberException;
import com.blockverse.app.exception.WorkSpaceNotFoundException;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.security.AuthService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.service.DocumentService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private DocumentService documentService;
        @MockitoBean
        private JwtUtil jwtUtil;
        @MockitoBean
        private UserRepo userRepo;
        @MockitoBean
        private UserDetailsService userDetailsService;
        @MockitoBean
        private AuthService authService;

        private DocumentResponse sampleDocResponse;

        @BeforeEach
        void setUp() {
                sampleDocResponse = DocumentResponse.builder()
                                .id(1).title("Test Doc").workspaceId(1).archived(false)
                                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                                .build();
        }

        // ========================================================================
        // POST /v1/documents/{workspaceId} — createDocument
        // ========================================================================

        @Nested
        @DisplayName("POST /v1/documents/{workspaceId}")
        class CreateDocumentTests {

                @Test
                @DisplayName("should return 200 with created document response")
                void createDocument_success() throws Exception {
                        when(documentService.createDocument(eq(1), any(CreateDocumentRequest.class)))
                                        .thenReturn(sampleDocResponse);

                        mockMvc.perform(post("/v1/documents/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "Test Doc"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.title").value("Test Doc"))
                                        .andExpect(jsonPath("$.workspaceId").value(1))
                                        .andExpect(jsonPath("$.archived").value(false));

                        verify(documentService).createDocument(eq(1), any(CreateDocumentRequest.class));
                }

                @Test
                @DisplayName("should return 404 when workspace does not exist")
                void createDocument_workspaceNotFound() throws Exception {
                        when(documentService.createDocument(eq(999), any(CreateDocumentRequest.class)))
                                        .thenThrow(new WorkSpaceNotFoundException("Workspace not found"));

                        mockMvc.perform(post("/v1/documents/999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "x"}
                                                        """))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void createDocument_nonMember() throws Exception {
                        when(documentService.createDocument(eq(1), any(CreateDocumentRequest.class)))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(post("/v1/documents/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "x"}
                                                        """))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // GET /v1/documents/{documentId} — getDocument
        // ========================================================================

        @Nested
        @DisplayName("GET /v1/documents/{documentId}")
        class GetDocumentTests {

                @Test
                @DisplayName("should return 200 with document response")
                void getDocument_success() throws Exception {
                        when(documentService.getDocument(1)).thenReturn(sampleDocResponse);

                        mockMvc.perform(get("/v1/documents/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.id").value(1))
                                        .andExpect(jsonPath("$.title").value("Test Doc"))
                                        .andExpect(jsonPath("$.workspaceId").value(1));
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void getDocument_notFound() throws Exception {
                        when(documentService.getDocument(999))
                                        .thenThrow(new DocumentNotFoundException("Document not found"));

                        mockMvc.perform(get("/v1/documents/999"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void getDocument_nonMember() throws Exception {
                        when(documentService.getDocument(1))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(get("/v1/documents/1"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // GET /v1/documents/{documentId}/details — getDocumentWithBlocks
        // ========================================================================

        @Nested
        @DisplayName("GET /v1/documents/{documentId}/details")
        class GetDocumentWithBlocksTests {

                @Test
                @DisplayName("should return 200 with document and its block tree")
                void getDocumentWithBlocks_success() throws Exception {
                        List<BlockResponse> blocks = List.of(
                                        BlockResponse.builder().id(1).documentId(1).parentId(null)
                                                        .type(BlockType.HEADING1).content("Title")
                                                        .position(BigInteger.valueOf(10000))
                                                        .children(new ArrayList<>()).build());
                        DocumentDetailsResponse details = DocumentDetailsResponse.builder()
                                        .document(sampleDocResponse).blocks(blocks).build();

                        when(documentService.getDocumentWithBlocks(1)).thenReturn(details);

                        mockMvc.perform(get("/v1/documents/1/details"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.document.id").value(1))
                                        .andExpect(jsonPath("$.document.title").value("Test Doc"))
                                        .andExpect(jsonPath("$.blocks", hasSize(1)))
                                        .andExpect(jsonPath("$.blocks[0].content").value("Title"))
                                        .andExpect(jsonPath("$.blocks[0].type").value("HEADING1"));
                }

                @Test
                @DisplayName("should return 200 with empty blocks list when document has no blocks")
                void getDocumentWithBlocks_noBlocks() throws Exception {
                        DocumentDetailsResponse details = DocumentDetailsResponse.builder()
                                        .document(sampleDocResponse).blocks(List.of()).build();

                        when(documentService.getDocumentWithBlocks(1)).thenReturn(details);

                        mockMvc.perform(get("/v1/documents/1/details"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.blocks", hasSize(0)));
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void getDocumentWithBlocks_notFound() throws Exception {
                        when(documentService.getDocumentWithBlocks(999))
                                        .thenThrow(new DocumentNotFoundException("Document not found"));

                        mockMvc.perform(get("/v1/documents/999/details"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void getDocumentWithBlocks_nonMember() throws Exception {
                        when(documentService.getDocumentWithBlocks(1))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(get("/v1/documents/1/details"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // GET /v1/documents/workspace/{workspaceId} — getDocumentsByWorkspace
        // ========================================================================

        @Nested
        @DisplayName("GET /v1/documents/workspace/{workspaceId}")
        class GetDocumentsByWorkspaceTests {

                @Test
                @DisplayName("should return 200 with list of non-archived documents")
                void getDocumentsByWorkspace_success() throws Exception {
                        DocumentResponse doc1 = DocumentResponse.builder()
                                        .id(1).title("Doc 1").workspaceId(1).archived(false).build();
                        DocumentResponse doc2 = DocumentResponse.builder()
                                        .id(2).title("Doc 2").workspaceId(1).archived(false).build();

                        when(documentService.getDocumentsByWorkspace(1))
                                        .thenReturn(List.of(doc1, doc2));

                        mockMvc.perform(get("/v1/documents/workspace/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[0].title").value("Doc 1"))
                                        .andExpect(jsonPath("$[1].title").value("Doc 2"));
                }

                @Test
                @DisplayName("should return 200 with empty list when no documents exist")
                void getDocumentsByWorkspace_empty() throws Exception {
                        when(documentService.getDocumentsByWorkspace(1))
                                        .thenReturn(List.of());

                        mockMvc.perform(get("/v1/documents/workspace/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(0)));
                }

                @Test
                @DisplayName("should return 404 when workspace does not exist")
                void getDocumentsByWorkspace_workspaceNotFound() throws Exception {
                        when(documentService.getDocumentsByWorkspace(999))
                                        .thenThrow(new WorkSpaceNotFoundException("Workspace not found"));

                        mockMvc.perform(get("/v1/documents/workspace/999"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void getDocumentsByWorkspace_nonMember() throws Exception {
                        when(documentService.getDocumentsByWorkspace(1))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(get("/v1/documents/workspace/1"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // PUT /v1/documents/{documentId} — updateDocument
        // ========================================================================

        @Nested
        @DisplayName("PUT /v1/documents/{documentId}")
        class UpdateDocumentTests {

                @Test
                @DisplayName("should return 200 with updated document response")
                void updateDocument_success() throws Exception {
                        DocumentResponse updatedResponse = DocumentResponse.builder()
                                        .id(1).title("Updated Title").workspaceId(1).archived(false).build();
                        when(documentService.updateDocument(eq(1), any(UpdateDocumentRequest.class)))
                                        .thenReturn(updatedResponse);

                        mockMvc.perform(put("/v1/documents/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "Updated Title"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.title").value("Updated Title"));

                        verify(documentService).updateDocument(eq(1), any(UpdateDocumentRequest.class));
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void updateDocument_notFound() throws Exception {
                        when(documentService.updateDocument(eq(999), any(UpdateDocumentRequest.class)))
                                        .thenThrow(new DocumentNotFoundException("Document not found"));

                        mockMvc.perform(put("/v1/documents/999")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "x"}
                                                        """))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void updateDocument_nonMember() throws Exception {
                        when(documentService.updateDocument(eq(1), any(UpdateDocumentRequest.class)))
                                        .thenThrow(new NotWorkSpaceMemberException("User is not a member"));

                        mockMvc.perform(put("/v1/documents/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "x"}
                                                        """))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // DELETE /v1/documents/{documentId} — archiveDocument
        // ========================================================================

        @Nested
        @DisplayName("DELETE /v1/documents/{documentId}")
        class ArchiveDocumentTests {

                @Test
                @DisplayName("should return 200 with success message when OWNER archives")
                void archiveDocument_success() throws Exception {
                        doNothing().when(documentService).archiveDocument(1);

                        mockMvc.perform(delete("/v1/documents/1"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document archived successfully"));

                        verify(documentService).archiveDocument(1);
                }

                @Test
                @DisplayName("should return 403 when user lacks permission to archive")
                void archiveDocument_forbidden() throws Exception {
                        doThrow(new InsufficientPermissionException(
                                        "Only workspace owners or admin can archive documents"))
                                        .when(documentService).archiveDocument(1);

                        mockMvc.perform(delete("/v1/documents/1"))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.error")
                                                        .value("Only workspace owners or admin can archive documents"));
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void archiveDocument_notFound() throws Exception {
                        doThrow(new DocumentNotFoundException("Document not found"))
                                        .when(documentService).archiveDocument(999);

                        mockMvc.perform(delete("/v1/documents/999"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void archiveDocument_nonMember() throws Exception {
                        doThrow(new NotWorkSpaceMemberException("User is not a member"))
                                        .when(documentService).archiveDocument(1);

                        mockMvc.perform(delete("/v1/documents/1"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // POST /v1/documents/{documentId}/restore — restoreDocument
        // ========================================================================

        @Nested
        @DisplayName("POST /v1/documents/{documentId}/restore")
        class RestoreDocumentTests {

                @Test
                @DisplayName("should return 200 with success message when OWNER restores")
                void restoreDocument_success() throws Exception {
                        doNothing().when(documentService).unarchiveDocument(1);

                        mockMvc.perform(post("/v1/documents/1/restore"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document restored successfully"));

                        verify(documentService).unarchiveDocument(1);
                }

                @Test
                @DisplayName("should return 403 when user lacks permission to restore")
                void restoreDocument_forbidden() throws Exception {
                        doThrow(new InsufficientPermissionException(
                                        "Only workspace owners or admin can unarchive documents"))
                                        .when(documentService).unarchiveDocument(1);

                        mockMvc.perform(post("/v1/documents/1/restore"))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.error").value(
                                                        "Only workspace owners or admin can unarchive documents"));
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void restoreDocument_notFound() throws Exception {
                        doThrow(new DocumentNotFoundException("Document not found"))
                                        .when(documentService).unarchiveDocument(999);

                        mockMvc.perform(post("/v1/documents/999/restore"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void restoreDocument_nonMember() throws Exception {
                        doThrow(new NotWorkSpaceMemberException("User is not a member"))
                                        .when(documentService).unarchiveDocument(1);

                        mockMvc.perform(post("/v1/documents/1/restore"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // DELETE /v1/documents/{documentId}/delete — deleteDocument (soft delete)
        // ========================================================================

        @Nested
        @DisplayName("DELETE /v1/documents/{documentId}/delete")
        class DeleteDocumentTests {

                @Test
                @DisplayName("should return 200 with success message when document is soft-deleted")
                void deleteDocument_success() throws Exception {
                        doNothing().when(documentService).deleteDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/delete"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document deleted successfully"));

                        verify(documentService).deleteDocument(1);
                }

                @Test
                @DisplayName("should return 403 when user lacks permission to delete")
                void deleteDocument_forbidden() throws Exception {
                        doThrow(new InsufficientPermissionException(
                                        "Only workspace owners or admin can delete documents"))
                                        .when(documentService).deleteDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/delete"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void deleteDocument_notFound() throws Exception {
                        doThrow(new DocumentNotFoundException("Document not found"))
                                        .when(documentService).deleteDocument(999);

                        mockMvc.perform(delete("/v1/documents/999/delete"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void deleteDocument_nonMember() throws Exception {
                        doThrow(new InsufficientPermissionException("User is not a member of this workspace"))
                                        .when(documentService).deleteDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/delete"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // DELETE /v1/documents/{documentId}/restore — restoreDeletedDocument
        // ========================================================================

        @Nested
        @DisplayName("DELETE /v1/documents/{documentId}/restore")
        class RestoreDeletedDocumentTests {

                @Test
                @DisplayName("should return 200 with success message when document is restored from trash")
                void restoreDeletedDocument_success() throws Exception {
                        doNothing().when(documentService).restoreDeletedDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/restore"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document restored successfully"));

                        verify(documentService).restoreDeletedDocument(1);
                }

                @Test
                @DisplayName("should return 403 when user lacks permission to restore")
                void restoreDeletedDocument_forbidden() throws Exception {
                        doThrow(new InsufficientPermissionException(
                                        "Only workspace owners or admin can restore documents"))
                                        .when(documentService).restoreDeletedDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/restore"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void restoreDeletedDocument_notFound() throws Exception {
                        doThrow(new DocumentNotFoundException("Document not found"))
                                        .when(documentService).restoreDeletedDocument(999);

                        mockMvc.perform(delete("/v1/documents/999/restore"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 400 when document is not in trash")
                void restoreDeletedDocument_notInTrash() throws Exception {
                        doThrow(new DocumentException("Document is not in trash"))
                                        .when(documentService).restoreDeletedDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/restore"))
                                        .andExpect(status().isBadRequest());
                }
        }

        // ========================================================================
        // DELETE /v1/documents/{documentId}/permanent — permanentDeleteDocument
        // ========================================================================

        @Nested
        @DisplayName("DELETE /v1/documents/{documentId}/permanent")
        class PermanentDeleteDocumentTests {

                @Test
                @DisplayName("should return 200 with success message when document is permanently deleted")
                void permanentDeleteDocument_success() throws Exception {
                        doNothing().when(documentService).permanentDeleteDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/permanent"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document permanently deleted successfully"));

                        verify(documentService).permanentDeleteDocument(1);
                }

                @Test
                @DisplayName("should return 403 when user lacks permission")
                void permanentDeleteDocument_forbidden() throws Exception {
                        doThrow(new InsufficientPermissionException(
                                        "Only workspace owners or admin can permanently delete documents"))
                                        .when(documentService).permanentDeleteDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/permanent"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void permanentDeleteDocument_notFound() throws Exception {
                        doThrow(new DocumentNotFoundException("Document not found"))
                                        .when(documentService).permanentDeleteDocument(999);

                        mockMvc.perform(delete("/v1/documents/999/permanent"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 400 when document is not in trash")
                void permanentDeleteDocument_notInTrash() throws Exception {
                        doThrow(new DocumentException("Document must be in trash before permanent deletion"))
                                        .when(documentService).permanentDeleteDocument(1);

                        mockMvc.perform(delete("/v1/documents/1/permanent"))
                                        .andExpect(status().isBadRequest());
                }
        }

        // ========================================================================
        // GET /v1/documents/workspace/{workspaceId}/trash —
        // getTrashDocumentsByWorkspace
        // ========================================================================

        @Nested
        @DisplayName("GET /v1/documents/workspace/{workspaceId}/trash")
        class GetTrashDocumentsByWorkspaceTests {

                @Test
                @DisplayName("should return 200 with list of trashed documents")
                void getTrashDocuments_success() throws Exception {
                        DocumentResponse trashedDoc = DocumentResponse.builder()
                                        .id(5).title("Trashed Doc").workspaceId(1).archived(false).build();

                        when(documentService.getTrashDocumentsByWorkspace(1))
                                        .thenReturn(List.of(trashedDoc));

                        mockMvc.perform(get("/v1/documents/workspace/1/trash"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(1)))
                                        .andExpect(jsonPath("$[0].title").value("Trashed Doc"));
                }

                @Test
                @DisplayName("should return 200 with empty list when no trashed documents exist")
                void getTrashDocuments_empty() throws Exception {
                        when(documentService.getTrashDocumentsByWorkspace(1))
                                        .thenReturn(List.of());

                        mockMvc.perform(get("/v1/documents/workspace/1/trash"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(0)));
                }

                @Test
                @DisplayName("should return 404 when workspace does not exist")
                void getTrashDocuments_workspaceNotFound() throws Exception {
                        when(documentService.getTrashDocumentsByWorkspace(999))
                                        .thenThrow(new WorkSpaceNotFoundException("Workspace not found"));

                        mockMvc.perform(get("/v1/documents/workspace/999/trash"))
                                        .andExpect(status().isNotFound());
                }

                @Test
                @DisplayName("should return 403 when user is not a workspace member")
                void getTrashDocuments_nonMember() throws Exception {
                        when(documentService.getTrashDocumentsByWorkspace(1))
                                        .thenThrow(new InsufficientPermissionException(
                                                        "User is not a member of this workspace"));

                        mockMvc.perform(get("/v1/documents/workspace/1/trash"))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // POST /v1/documents/{documentId}/share — createShareLink
        // ========================================================================

        @Nested
        @DisplayName("POST /v1/documents/{documentId}/share")
        class CreateShareLinkTests {

                @Test
                @DisplayName("should return 200 with share link response")
                void createShareLink_success() throws Exception {
                        com.blockverse.app.dto.document.ShareLinkResponse response = 
                                new com.blockverse.app.dto.document.ShareLinkResponse("http://localhost:8080/share/token123", LocalDateTime.now().plusMinutes(60));
                        when(documentService.createShareLink(eq(1), eq(60))).thenReturn(response);

                        mockMvc.perform(post("/v1/documents/1/share")
                                        .param("expiryMinutes", "60"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.url").value("http://localhost:8080/share/token123"));
                }

                @Test
                @DisplayName("should return 403 when user lacks permission")
                void createShareLink_forbidden() throws Exception {
                        when(documentService.createShareLink(eq(1), eq(60)))
                                .thenThrow(new InsufficientPermissionException("Only workspace owners or admin can create share links"));

                        mockMvc.perform(post("/v1/documents/1/share")
                                        .param("expiryMinutes", "60"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("should return 404 when document does not exist")
                void createShareLink_notFound() throws Exception {
                        when(documentService.createShareLink(eq(999), eq(60)))
                                .thenThrow(new DocumentNotFoundException("Document not found"));

                        mockMvc.perform(post("/v1/documents/999/share")
                                        .param("expiryMinutes", "60"))
                                        .andExpect(status().isNotFound());
                }
        }
}
