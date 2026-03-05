package com.blockverse.app.mapper;

import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.entity.Document;

public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        if (document == null) return null;

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .workspaceId(document.getWorkSpace().getId())
                .archived(document.isArchived())
                .createdAt(document.getCreatedAt())
                .build();
    }
}