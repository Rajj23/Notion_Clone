package com.blockverse.app.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class DocumentResponse {
    
    private int id;
    private String title;
    private int workspaceId;
    private boolean archived;
    private LocalDateTime createdAt;
    
}
