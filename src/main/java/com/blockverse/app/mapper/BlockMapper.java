package com.blockverse.app.mapper;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.entity.Block;

import java.util.ArrayList;

public class BlockMapper {
    public static BlockResponse toBlockResponse(Block block) {
        if (block == null) return null;

        return BlockResponse.builder()
                .id(block.getId())
                .documentId(block.getDocument().getId())
                .parentId(block.getParent() != null ? block.getParent().getId() : null)
                .type(block.getType())
                .content(block.getContent())
                .position(block.getPosition())
                .children(new ArrayList<>())
                .build();
    }
}