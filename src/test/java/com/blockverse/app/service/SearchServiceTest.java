package com.blockverse.app.service;

import com.blockverse.app.dto.SearchResponse;
import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.entity.Block;
import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.repo.BlockRepo;
import com.blockverse.app.repo.DocumentRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private DocumentRepo documentRepo;
    
    @Mock
    private BlockRepo blockRepo;
    
    @Mock
    private DocumentMapper documentMapper;
    
    @Mock
    private com.blockverse.app.mapper.BlockMapper blockMapper;

    @InjectMocks
    private SearchService searchService;

    private Document testDocument;
    private Block testBlock;
    private DocumentResponse testDocResponse;

    @BeforeEach
    void setUp() {
        WorkSpace ws = WorkSpace.builder().id(1).build();
        testDocument = Document.builder().id(1).title("Keyword title").workSpace(ws).build();
        testBlock = Block.builder().id(1).document(testDocument).type(BlockType.PARAGRAPH)
                .content("Keyword content").position(BigInteger.ONE).build();
        
        testDocResponse = DocumentResponse.builder().id(1).title("Keyword title").workspaceId(1).build();
    }

    @Test
    @DisplayName("should search and map documents and blocks successfully")
    void search_success() {
        when(documentRepo.searchDocuments("Keyword", 1)).thenReturn(List.of(testDocument));
        when(blockRepo.searchBlocks("Keyword", 1)).thenReturn(List.of(testBlock));
        when(documentMapper.toResponse(testDocument)).thenReturn(testDocResponse);
        
        BlockResponse blockResponse = BlockResponse.builder().id(1).content("Keyword content").build();
        when(blockMapper.toBlockResponse(testBlock)).thenReturn(blockResponse);

        SearchResponse response = searchService.search("Keyword", 1);

        assertNotNull(response);
        assertEquals(1, response.getDocuments().size());
        assertEquals("Keyword title", response.getDocuments().get(0).getTitle());
        
        assertEquals(1, response.getBlocks().size());
        assertEquals("Keyword content", response.getBlocks().get(0).getContent());
    }
}
