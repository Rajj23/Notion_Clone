package com.blockverse.app.service;

import com.blockverse.app.dto.SearchResponse;
import com.blockverse.app.entity.Block;
import com.blockverse.app.entity.Document;
import com.blockverse.app.mapper.BlockMapper;
import com.blockverse.app.mapper.DocumentMapper;
import com.blockverse.app.repo.BlockRepo;
import com.blockverse.app.repo.DocumentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final DocumentRepo documentRepo;
    private final BlockRepo blockRepo;
    private final DocumentMapper documentMapper;
    private final BlockMapper blockMapper;

    public SearchResponse search(String keyword, int workSpaceId){
        List<Document> documents = documentRepo.searchDocuments(keyword, workSpaceId);
        List<Block> blocks = blockRepo.searchBlocks(keyword, workSpaceId);

        return SearchResponse.builder()
                .documents(documents.stream().map(documentMapper::toResponse).toList())
                .blocks(blocks.stream().map(blockMapper::toBlockResponse).toList())
                .build();
    }
}