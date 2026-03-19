package com.blockverse.app.controller;

import com.blockverse.app.dto.SearchResponse;
import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.security.AuthService;
import com.blockverse.app.security.JwtUtil;
import com.blockverse.app.repo.UserRepo;
import com.blockverse.app.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepo userRepo;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("should return 200 with SearchResponse")
    void search_success() throws Exception {
        DocumentResponse doc = DocumentResponse.builder()
                .id(1).title("Test Document").workspaceId(1).build();
        BlockResponse block = BlockResponse.builder()
                .id(1).documentId(1).type(BlockType.PARAGRAPH).content("Test Block Content")
                .position(BigInteger.ONE).build();
        
        SearchResponse response = SearchResponse.builder()
                .documents(List.of(doc))
                .blocks(List.of(block))
                .build();

        when(searchService.search("Test", 1)).thenReturn(response);

        mockMvc.perform(get("/v1/search")
                .param("keyword", "Test")
                .param("workSpaceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents", hasSize(1)))
                .andExpect(jsonPath("$.documents[0].title").value("Test Document"))
                .andExpect(jsonPath("$.blocks", hasSize(1)))
                .andExpect(jsonPath("$.blocks[0].content").value("Test Block Content"));
    }
}
