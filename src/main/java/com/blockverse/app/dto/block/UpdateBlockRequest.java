package com.blockverse.app.dto.block;

import com.blockverse.app.enums.BlockType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class UpdateBlockRequest {
    private BlockType type;
    private String content;
}
