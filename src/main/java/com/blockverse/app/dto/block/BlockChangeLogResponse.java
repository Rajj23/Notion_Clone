package com.blockverse.app.dto.block;

import com.blockverse.app.enums.BlockOperationType;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlockChangeLogResponse {
    private int id;
    private int documentId;
    private Integer blockId;
    private BlockOperationType operationType; 
    private String oldContent;
    private String newContent;
    private Long versionNumber;
    private LocalDateTime createdAt;
}
