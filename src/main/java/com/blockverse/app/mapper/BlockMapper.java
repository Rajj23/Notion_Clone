package com.blockverse.app.mapper;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.entity.Block;
import com.blockverse.app.enums.BlockType;
import com.blockverse.app.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
@Component
@RequiredArgsConstructor
public class BlockMapper {
    
    private final S3Service s3Service;
    
    public BlockResponse toBlockResponse(Block block) {
        if (block == null) return null;

        BlockResponse response =  BlockResponse.builder()
                .id(block.getId())
                .documentId(block.getDocument().getId())
                .parentId(block.getParent() != null ? block.getParent().getId() : null)
                .type(block.getType())
                .content(block.getContent())
                .position(block.getPosition())
                .children(new ArrayList<>())
                .build();
        
        if(block.getType() == BlockType.IMAGE){
            response.setFileUrl(s3Service.generateUrl(block.getContent()));
            response.setContent(null);
        }
        else{
            response.setContent(block.getContent());
        }
        return response;
    }
}