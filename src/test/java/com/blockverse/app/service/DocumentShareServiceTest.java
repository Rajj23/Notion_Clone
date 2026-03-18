package com.blockverse.app.service;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.DocumentShare;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.repo.DocumentShareRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentShareServiceTest {

    @Mock
    private DocumentShareRepo documentShareRepo;
    @Mock
    private BlockService blockService;
    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentShareService documentShareService;

    private DocumentShare share;
    private Document document;

    @BeforeEach
    void setUp() {
        document = Document.builder().id(1).title("Shared Doc").build();
        share = DocumentShare.builder()
                .id(1)
                .document(document)
                .token("valid_token")
                .active(true)
                .expiryTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    @DisplayName("should get shared document and blocks with valid token")
    void getSharedDocument_success() {
        when(documentShareRepo.findByTokenAndActiveTrue("valid_token")).thenReturn(Optional.of(share));
        DocumentResponse docResp = DocumentResponse.builder().id(1).title("Shared Doc").build();
        when(documentMapper.toResponse(document)).thenReturn(docResp);
        when(blockService.getBlocksForDocumentWithoutAuth(1)).thenReturn(List.of());

        DocumentDetailsResponse response = documentShareService.getSharedDocument("valid_token");

        assertNotNull(response);
        assertEquals(docResp, response.getDocument());
        assertTrue(response.getBlocks().isEmpty());
    }

    @Test
    @DisplayName("should reject invalid or inactive token")
    void getSharedDocument_invalidToken() {
        when(documentShareRepo.findByTokenAndActiveTrue("invalid_token")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> documentShareService.getSharedDocument("invalid_token"));
    }

    @Test
    @DisplayName("should reject expired token")
    void getSharedDocument_expiredToken() {
        share.setExpiryTime(LocalDateTime.now().minusMinutes(5));
        when(documentShareRepo.findByTokenAndActiveTrue("valid_token")).thenReturn(Optional.of(share));

        assertThrows(IllegalArgumentException.class, () -> documentShareService.getSharedDocument("valid_token"));
    }
    
    @Test
    @DisplayName("should succeed when token has no expiry")
    void getSharedDocument_noExpiry() {
        share.setExpiryTime(null);
        when(documentShareRepo.findByTokenAndActiveTrue("valid_token")).thenReturn(Optional.of(share));
        DocumentResponse docResp = DocumentResponse.builder().id(1).title("Shared Doc").build();
        when(documentMapper.toResponse(document)).thenReturn(docResp);
        when(blockService.getBlocksForDocumentWithoutAuth(1)).thenReturn(List.of());

        DocumentDetailsResponse response = documentShareService.getSharedDocument("valid_token");

        assertNotNull(response);
        assertEquals(docResp, response.getDocument());
    }
}
