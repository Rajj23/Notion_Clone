package com.blockverse.app.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;
    private int workspaceId;
    private int documentId;

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

    private Long getDocumentVersion(String token, int documentId) throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/documents/" + documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Integer version = JsonPath.read(result.getResponse().getContentAsString(), "$.version");
        return version == null ? null : version.longValue();
    }

    private int createBlockAndGetId(String token, int documentId, String type, String content) throws Exception {
        Long version = getDocumentVersion(token, documentId);
        String versionJson = version == null ? "null" : String.valueOf(version);
        MvcResult result = mockMvc.perform(post("/v1/blocks/" + documentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId": null, "type": "%s", "content": "%s", "documentVersion": %s}
                                """.formatted(type, content, versionJson)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    @BeforeEach
    void setup() throws Exception {
        token = signupAndGetToken("Search Tester", "search@test.com", "pass123");
        workspaceId = createWorkspaceAndGetId(token, "Search WS", "PRIVATE");
        documentId = createDocumentAndGetId(token, workspaceId, "UniqueTitleKeyword");
        createBlockAndGetId(token, documentId, "PARAGRAPH", "UniqueBlockKeyword content here.");
    }

    @Test
    @DisplayName("Should successfully find documents and blocks by keyword in the workspace")
    void search_integration_success() throws Exception {
        // Test searching for the document title
        mockMvc.perform(get("/v1/search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "UniqueTitleKeyword")
                        .param("workSpaceId", String.valueOf(workspaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(1)))
                .andExpect(jsonPath("$.documents[0].title").value("UniqueTitleKeyword"));
                
        // Test searching for the block content
        mockMvc.perform(get("/v1/search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "UniqueBlockKeyword")
                        .param("workSpaceId", String.valueOf(workspaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks", hasSize(1)))
                .andExpect(jsonPath("$.blocks[0].content").value("UniqueBlockKeyword content here."));
    }
}
