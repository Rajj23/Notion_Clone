package com.blockverse.app.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        // ── Helper: signup and extract accessToken ───────────────────────────────
        private String signupAndGetToken(String name, String email, String password) throws Exception {
                MvcResult result = mockMvc.perform(post("/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"name": "%s", "email": "%s", "password": "%s"}
                                                """.formatted(name, email, password)))
                                .andExpect(status().isOk())
                                .andReturn();
                return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        }

        // ── Helper: login and extract accessToken ────────────────────────────────
        private String loginAndGetToken(String email, String password) throws Exception {
                MvcResult result = mockMvc.perform(post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email": "%s", "password": "%s"}
                                                """.formatted(email, password)))
                                .andExpect(status().isOk())
                                .andReturn();
                return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        }

        // ── Helper: create workspace, return its ID ──────────────────────────────
        private int createWorkspaceAndGetId(String token, String name, String type) throws Exception {
                mockMvc.perform(post("/v1/workspaces/create")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"name": "%s", "workSpaceType": "%s"}
                                                """.formatted(name, type)))
                                .andExpect(status().isOk());

                MvcResult listResult = mockMvc.perform(get("/v1/workspaces/all")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andReturn();
                return JsonPath.read(listResult.getResponse().getContentAsString(), "$[0].id");
        }

        // ── Helper: create document, return its ID ───────────────────────────────
        private int createDocumentAndGetId(String token, int workspaceId, String title) throws Exception {
                MvcResult result = mockMvc.perform(post("/v1/documents/" + workspaceId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"title": "%s"}
                                                """.formatted(title)))
                                .andExpect(status().isOk())
                                .andReturn();
                return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        }

        // ── Helper: get current document version ───────────────────────────────────
        private Long getDocumentVersion(String token, int documentId) throws Exception {
                MvcResult result = mockMvc.perform(get("/v1/documents/" + documentId)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andReturn();
                Integer version = JsonPath.read(result.getResponse().getContentAsString(), "$.version");
                return version == null ? null : version.longValue();
        }

        // ── Helper: create block, return its ID ──────────────────────────────────
        private int createBlockAndGetId(String token, int documentId, Integer parentId,
                        String type, String content) throws Exception {
                String parentJson = parentId == null ? "null" : String.valueOf(parentId);
                Long version = getDocumentVersion(token, documentId);
                String versionJson = version == null ? "null" : String.valueOf(version);
                MvcResult result = mockMvc.perform(post("/v1/blocks/" + documentId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"parentId": %s, "type": "%s", "content": "%s", "documentVersion": %s}
                                                """.formatted(parentJson, type, content, versionJson)))
                                .andExpect(status().isOk())
                                .andReturn();
                return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        }

        // ========================================================================
        // Auth Flow
        // ========================================================================

        @Nested
        @DisplayName("Auth Flow")
        class AuthFlowTests {

                @Test
                @DisplayName("signup should return access and refresh tokens")
                void signup_success() throws Exception {
                        mockMvc.perform(post("/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"name": "Alice", "email": "alice@test.com", "password": "secret123"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
                }

                @Test
                @DisplayName("duplicate email signup should return 409 conflict")
                void signup_duplicateEmail() throws Exception {
                        signupAndGetToken("Alice", "alice@test.com", "secret123");

                        mockMvc.perform(post("/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"name": "Alice2", "email": "alice@test.com", "password": "other"}
                                                        """))
                                        .andExpect(status().isConflict());
                }

                @Test
                @DisplayName("login should return tokens for existing user")
                void login_success() throws Exception {
                        signupAndGetToken("Alice", "alice@test.com", "secret123");

                        mockMvc.perform(post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "alice@test.com", "password": "secret123"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
                }

                @Test
                @DisplayName("login with wrong password should fail")
                void login_wrongPassword() throws Exception {
                        signupAndGetToken("Alice", "alice@test.com", "secret123");

                        mockMvc.perform(post("/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "alice@test.com", "password": "wrong"}
                                                        """))
                                        .andExpect(status().isInternalServerError());
                }

                @Test
                @DisplayName("unauthenticated request to protected endpoint should return 403")
                void unauthenticated_request() throws Exception {
                        mockMvc.perform(get("/v1/workspaces/all"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("signup with invalid email format should return 400")
                void signup_invalidEmail() throws Exception {
                        mockMvc.perform(post("/v1/auth/signup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"name": "Alice", "email": "not-an-email", "password": "secret123"}
                                                        """))
                                        .andExpect(status().isBadRequest());
                }
        }

        // ========================================================================
        // Workspace Flow
        // ========================================================================

        @Nested
        @DisplayName("Workspace Flow")
        class WorkspaceFlowTests {

                @Test
                @DisplayName("should create workspace and retrieve it")
                void createAndGetWorkspace() throws Exception {
                        String token = signupAndGetToken("Alice", "alice@test.com", "secret123");

                        mockMvc.perform(post("/v1/workspaces/create")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"name": "My Workspace", "workSpaceType": "PRIVATE"}
                                                        """))
                                        .andExpect(status().isOk());

                        mockMvc.perform(get("/v1/workspaces/all")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(1)))
                                        .andExpect(jsonPath("$[0].name").value("My Workspace"))
                                        .andExpect(jsonPath("$[0].workSpaceType").value("PRIVATE"))
                                        .andExpect(jsonPath("$[0].userRoleInWorkSpace").value("OWNER"));
                }

                @Test
                @DisplayName("should update workspace name")
                void updateWorkspace() throws Exception {
                        String token = signupAndGetToken("Alice", "alice@test.com", "secret123");
                        int wsId = createWorkspaceAndGetId(token, "Old Name", "TEAM");

                        mockMvc.perform(put("/v1/workspaces/update/" + wsId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"name": "New Name", "workSpaceType": "TEAM"}
                                                        """))
                                        .andExpect(status().isOk());

                        mockMvc.perform(get("/v1/workspaces/" + wsId)
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("New Name"));
                }

                @Test
                @DisplayName("should delete workspace — only owner can delete")
                void deleteWorkspace() throws Exception {
                        String token = signupAndGetToken("Alice", "alice@test.com", "secret123");
                        int wsId = createWorkspaceAndGetId(token, "To Delete", "PRIVATE");

                        mockMvc.perform(delete("/v1/workspaces/delete/" + wsId)
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("WorkSpace deleted successfully"));
                }
        }

        // ========================================================================
        // Workspace Member Flow
        // ========================================================================

        @Nested
        @DisplayName("Workspace Member Flow")
        class MemberFlowTests {

                private String ownerToken;
                private int workspaceId;

                @BeforeEach
                void setupWorkspace() throws Exception {
                        ownerToken = signupAndGetToken("Owner", "owner@test.com", "pass123");
                        signupAndGetToken("Member", "member@test.com", "pass123");

                        workspaceId = createWorkspaceAndGetId(ownerToken, "Team WS", "TEAM");
                }

                @Test
                @DisplayName("should add member, count members, then remove member")
                void addAndRemoveMember() throws Exception {
                        mockMvc.perform(post("/v1/workspace/member/" + workspaceId + "/add")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "member@test.com", "role": "MEMBER"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Member added to workspace successfully"));

                        mockMvc.perform(get("/v1/workspace/member/" + workspaceId + "/count-members")
                                        .header("Authorization", "Bearer " + ownerToken))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("2"));

                        mockMvc.perform(delete("/v1/workspace/member/" + workspaceId + "/remove")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .param("email", "member@test.com"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Member removed from workspace successfully"));

                        mockMvc.perform(get("/v1/workspace/member/" + workspaceId + "/count-members")
                                        .header("Authorization", "Bearer " + ownerToken))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("1"));
                }

                @Test
                @DisplayName("should change member role")
                void changeMemberRole() throws Exception {
                        mockMvc.perform(post("/v1/workspace/member/" + workspaceId + "/add")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "member@test.com", "role": "MEMBER"}
                                                        """))
                                        .andExpect(status().isOk());

                        mockMvc.perform(post("/v1/workspace/member/" + workspaceId + "/change-role")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "member@test.com", "role": "ADMIN"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Member role changed successfully"));
                }

                @Test
                @DisplayName("member should be able to leave workspace")
                void memberLeaves() throws Exception {
                        mockMvc.perform(post("/v1/workspace/member/" + workspaceId + "/add")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "member@test.com", "role": "MEMBER"}
                                                        """))
                                        .andExpect(status().isOk());

                        String memberToken = loginAndGetToken("member@test.com", "pass123");

                        mockMvc.perform(post("/v1/workspace/member/" + workspaceId + "/leave")
                                        .header("Authorization", "Bearer " + memberToken))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Left workspace successfully"));

                        mockMvc.perform(get("/v1/workspace/member/" + workspaceId + "/count-members")
                                        .header("Authorization", "Bearer " + ownerToken))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("1"));
                }

                @Test
                @DisplayName("non-member should not access workspace resources")
                void nonMember_cannotAccessDocuments() throws Exception {
                        String nonMemberToken = loginAndGetToken("member@test.com", "pass123");

                        mockMvc.perform(get("/v1/documents/workspace/" + workspaceId)
                                        .header("Authorization", "Bearer " + nonMemberToken))
                                        .andExpect(status().isForbidden());
                }
        }

        // ========================================================================
        // Document Flow
        // ========================================================================

        @Nested
        @DisplayName("Document Flow")
        class DocumentFlowTests {

                private String token;
                private int workspaceId;

                @BeforeEach
                void setupWorkspace() throws Exception {
                        token = signupAndGetToken("Alice", "alice@test.com", "secret123");
                        workspaceId = createWorkspaceAndGetId(token, "Doc WS", "PRIVATE");
                }

                @Test
                @DisplayName("should create document, retrieve it, update title, archive, and restore")
                void fullDocumentLifecycle() throws Exception {
                        int docId = createDocumentAndGetId(token, workspaceId, "My Document");

                        mockMvc.perform(get("/v1/documents/" + docId)
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.title").value("My Document"))
                                        .andExpect(jsonPath("$.workspaceId").value(workspaceId))
                                        .andExpect(jsonPath("$.archived").value(false));

                        mockMvc.perform(put("/v1/documents/" + docId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"title": "Updated Title"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.title").value("Updated Title"));

                        mockMvc.perform(delete("/v1/documents/" + docId)
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document archived successfully"));

                        mockMvc.perform(post("/v1/documents/" + docId + "/restore")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Document restored successfully"));
                }

                @Test
                @DisplayName("should list documents for workspace")
                void listDocumentsByWorkspace() throws Exception {
                        createDocumentAndGetId(token, workspaceId, "Doc 1");
                        createDocumentAndGetId(token, workspaceId, "Doc 2");

                        mockMvc.perform(get("/v1/documents/workspace/" + workspaceId)
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)));
                }

                @Test
                @DisplayName("should get document details with blocks")
                void getDocumentDetails() throws Exception {
                        int docId = createDocumentAndGetId(token, workspaceId, "Detail Doc");

                        mockMvc.perform(get("/v1/documents/" + docId + "/details")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.document.title").value("Detail Doc"))
                                        .andExpect(jsonPath("$.blocks").isArray());
                }
        }

        // ========================================================================
        // Block Flow
        // ========================================================================

        @Nested
        @DisplayName("Block Flow")
        class BlockFlowTests {

                private String token;
                private int documentId;

                @BeforeEach
                void setupDocumentInWorkspace() throws Exception {
                        token = signupAndGetToken("Alice", "alice@test.com", "secret123");
                        int workspaceId = createWorkspaceAndGetId(token, "Block WS", "PRIVATE");
                        documentId = createDocumentAndGetId(token, workspaceId, "Block Doc");
                }

                @Test
                @DisplayName("should create block with correct default position 10000")
                void createBlock() throws Exception {
                        Long version = getDocumentVersion(token, documentId);
                        String versionJson = version == null ? "null" : String.valueOf(version);
                        mockMvc.perform(post("/v1/blocks/" + documentId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"parentId": null, "type": "PARAGRAPH", "content": "Hello World", "documentVersion": %s}
                                                        """
                                                        .formatted(versionJson)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.type").value("PARAGRAPH"))
                                        .andExpect(jsonPath("$.content").value("Hello World"))
                                        .andExpect(jsonPath("$.position").value(10000))
                                        .andExpect(jsonPath("$.documentId").value(documentId));
                }

                @Test
                @DisplayName("second block should get position 20000 (10000 increment)")
                void createBlock_positionIncrement() throws Exception {
                        Long v1 = getDocumentVersion(token, documentId);
                        String v1Json = v1 == null ? "null" : String.valueOf(v1);
                        mockMvc.perform(post("/v1/blocks/" + documentId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"parentId": null, "type": "HEADING1", "content": "First", "documentVersion": %s}
                                                        """
                                                        .formatted(v1Json)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.position").value(10000));

                        Long v2 = getDocumentVersion(token, documentId);
                        String v2Json = v2 == null ? "null" : String.valueOf(v2);
                        mockMvc.perform(post("/v1/blocks/" + documentId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"parentId": null, "type": "PARAGRAPH", "content": "Second", "documentVersion": %s}
                                                        """
                                                        .formatted(v2Json)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.position").value(20000));
                }

                @Test
                @DisplayName("should create child block under parent")
                void createChildBlock() throws Exception {
                        int parentId = createBlockAndGetId(token, documentId, null, "HEADING1", "Parent");

                        Long version = getDocumentVersion(token, documentId);
                        String versionJson = version == null ? "null" : String.valueOf(version);
                        mockMvc.perform(post("/v1/blocks/" + documentId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"parentId": %d, "type": "BULLET", "content": "Child item", "documentVersion": %s}
                                                        """
                                                        .formatted(parentId, versionJson)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.parentId").value(parentId))
                                        .andExpect(jsonPath("$.type").value("BULLET"));
                }

                @Test
                @DisplayName("should update block content and type")
                void updateBlock() throws Exception {
                        int blockId = createBlockAndGetId(token, documentId, null, "PARAGRAPH", "Old Content");

                        Long version = getDocumentVersion(token, documentId);
                        String versionJson = version == null ? "null" : String.valueOf(version);
                        mockMvc.perform(put("/v1/blocks/" + blockId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"type": "HEADING2", "content": "New Content", "documentVersion": %s}
                                                        """
                                                        .formatted(versionJson)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.type").value("HEADING2"))
                                        .andExpect(jsonPath("$.content").value("New Content"));
                }

                @Test
                @DisplayName("should soft-delete block — block disappears from document tree")
                void deleteBlock() throws Exception {
                        int blockId = createBlockAndGetId(token, documentId, null, "PARAGRAPH", "To Delete");

                        Long version = getDocumentVersion(token, documentId);
                        String versionJson = version == null ? "null" : String.valueOf(version);
                        mockMvc.perform(delete("/v1/blocks/" + blockId)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"documentVersion": %s}
                                                        """.formatted(versionJson)))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("Block deleted successfully"));

                        mockMvc.perform(get("/v1/documents/" + documentId + "/details")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.blocks", hasSize(0)));
                }

                @Test
                @DisplayName("should get children of a block")
                void getChildren() throws Exception {
                        int parentId = createBlockAndGetId(token, documentId, null, "HEADING1", "Parent");
                        createBlockAndGetId(token, documentId, parentId, "BULLET", "Child 1");
                        createBlockAndGetId(token, documentId, parentId, "BULLET", "Child 2");

                        mockMvc.perform(get("/v1/blocks/" + parentId + "/children")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[0].content").value("Child 1"))
                                        .andExpect(jsonPath("$[1].content").value("Child 2"));
                }

                @Test
                @DisplayName("should move block to a new parent")
                void moveBlock() throws Exception {
                        int parent1Id = createBlockAndGetId(token, documentId, null, "HEADING1", "Parent 1");
                        int parent2Id = createBlockAndGetId(token, documentId, null, "HEADING1", "Parent 2");
                        int childId = createBlockAndGetId(token, documentId, parent1Id, "PARAGRAPH", "Moving child");

                        Long version = getDocumentVersion(token, documentId);
                        String versionJson = version == null ? "null" : String.valueOf(version);
                        mockMvc.perform(put("/v1/blocks/" + childId + "/move")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"newParentId": %d, "newPosition": 10000, "documentVersion": %s}
                                                        """.formatted(parent2Id, versionJson)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.parentId").value(parent2Id))
                                        .andExpect(jsonPath("$.position").value(10000));

                        mockMvc.perform(get("/v1/blocks/" + parent1Id + "/children")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(jsonPath("$", hasSize(0)));

                        mockMvc.perform(get("/v1/blocks/" + parent2Id + "/children")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(jsonPath("$", hasSize(1)))
                                        .andExpect(jsonPath("$[0].content").value("Moving child"));
                }
        }

        // ========================================================================
        // Cross-cutting Security Tests
        // ========================================================================

        @Nested
        @DisplayName("Cross-cutting Security")
        class SecurityTests {

                @Test
                @DisplayName("member should access documents, non-member should be denied")
                void memberAccess_vs_nonMemberDenied() throws Exception {
                        String ownerToken = signupAndGetToken("Owner", "owner@test.com", "pass123");
                        signupAndGetToken("Member", "member@test.com", "pass123");
                        signupAndGetToken("Outsider", "outsider@test.com", "pass123");

                        int wsId = createWorkspaceAndGetId(ownerToken, "Secure WS", "TEAM");

                        mockMvc.perform(post("/v1/workspace/member/" + wsId + "/add")
                                        .header("Authorization", "Bearer " + ownerToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email": "member@test.com", "role": "MEMBER"}
                                                        """))
                                        .andExpect(status().isOk());

                        createDocumentAndGetId(ownerToken, wsId, "Secure Doc");

                        String memberToken = loginAndGetToken("member@test.com", "pass123");
                        mockMvc.perform(get("/v1/documents/workspace/" + wsId)
                                        .header("Authorization", "Bearer " + memberToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(1)));

                        String outsiderToken = loginAndGetToken("outsider@test.com", "pass123");
                        mockMvc.perform(get("/v1/documents/workspace/" + wsId)
                                        .header("Authorization", "Bearer " + outsiderToken))
                                        .andExpect(status().isForbidden());
                }
        }
}
