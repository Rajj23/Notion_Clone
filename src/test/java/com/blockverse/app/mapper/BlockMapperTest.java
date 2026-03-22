package com.blockverse.app.mapper;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.entity.Block;
import com.blockverse.app.entity.Document;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockMapperTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private BlockMapper blockMapper;

    private Block sampleBlock;
    private Document sampleDocument;

    @BeforeEach
    void setUp() {
        sampleDocument = Document.builder().id(1).build();
        sampleBlock = Block.builder()
                .id(10)
                .document(sampleDocument)
                .type(BlockType.PARAGRAPH)
                .content("Hello")
                .position(BigInteger.valueOf(1000))
                .children(new ArrayList<>())
                .build();
    }

    @Test
    void toBlockResponse_nullBlock_returnsNull() {
        assertNull(blockMapper.toBlockResponse(null));
    }

    @Test
    void toBlockResponse_nonImageBlock_retainsContent() {
        BlockResponse response = blockMapper.toBlockResponse(sampleBlock);

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals(1, response.getDocumentId());
        assertEquals("Hello", response.getContent());
        assertNull(response.getFileUrl());
        verifyNoInteractions(s3Service);
    }

    @Test
    void toBlockResponse_imageBlock_generatesS3UrlAndSetsContentToNull() {
        sampleBlock.setType(BlockType.IMAGE);
        sampleBlock.setContent("image-key.jpg");

        String expectedUrl = "https://s3.url/image-key.jpg";
        when(s3Service.generateUrl("image-key.jpg")).thenReturn(expectedUrl);

        BlockResponse response = blockMapper.toBlockResponse(sampleBlock);

        assertNotNull(response);
        assertEquals(10, response.getId());
        assertEquals(1, response.getDocumentId());
        assertNull(response.getContent());
        assertEquals(expectedUrl, response.getFileUrl());
        
        verify(s3Service).generateUrl("image-key.jpg");
    }
}
