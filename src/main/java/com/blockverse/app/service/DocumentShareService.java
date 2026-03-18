package com.blockverse.app.service;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.DocumentShare;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.repo.DocumentShareRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentShareService {

    private final DocumentShareRepo documentShareRepo;
    private final BlockService blockService;
    private final DocumentMapper documentMapper;

    public DocumentDetailsResponse getSharedDocument(String token) {
        DocumentShare share = documentShareRepo
                .findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired share token"));
        
        if(share.getExpiryTime() != null && 
            share.getExpiryTime().isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("Link has expired");
        }

        Document document = share.getDocument();

        List<BlockResponse> blocks = blockService.getBlocksForDocumentWithoutAuth(document.getId());
        
        return DocumentDetailsResponse.builder()
                .document(documentMapper.toResponse(document))
                .blocks(blocks)
                .build();
    }
}
