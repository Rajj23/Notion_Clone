package com.blockverse.app.dto.document;

import com.blockverse.app.enums.AuditActionType;
import com.blockverse.app.enums.AuditEntityType;
import com.blockverse.app.enums.BlockOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DocumentEvent {

    private int documentId;
    
    private AuditEntityType entityType;
    
    private Enum<?> action;
    
    private Object payload;
}
