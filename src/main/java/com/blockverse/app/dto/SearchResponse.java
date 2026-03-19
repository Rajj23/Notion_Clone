package com.blockverse.app.dto;

import com.blockverse.app.dto.block.BlockResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SearchResponse {
    private List<DocumentResponse> documents;
    private List<BlockResponse> blocks;
}
