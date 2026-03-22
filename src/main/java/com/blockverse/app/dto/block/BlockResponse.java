package com.blockverse.app.dto.block;

import com.blockverse.app.enums.BlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockResponse {
    private int id;
    private int documentId;
    private Integer parentId;
    private BlockType type;
    private String content;
    private String fileUrl;
    private BigInteger position;
    private List<BlockResponse> children;

}
