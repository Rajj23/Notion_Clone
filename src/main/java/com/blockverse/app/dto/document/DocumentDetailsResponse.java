package com.blockverse.app.dto.document;

import com.blockverse.app.dto.block.BlockResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class DocumentDetailsResponse {
    private DocumentResponse document;
    private List<BlockResponse> blocks;
}