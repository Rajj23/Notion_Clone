package com.blockverse.app.entity;

import com.blockverse.app.enums.AuditActionType;
import com.blockverse.app.enums.AuditEntityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private int workSpaceId;
    private int userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEntityType entityType;
    
    private int entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditActionType actionType;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
