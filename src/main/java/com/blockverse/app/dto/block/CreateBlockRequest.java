package com.blockverse.app.dto.block;

import com.blockverse.app.enums.BlockType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlockRequest {
    private Integer parentId;
    private BlockType type;
    private String content;
}
