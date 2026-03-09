package com.blockverse.app.entity;

import com.blockverse.app.enums.BlockOperationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockChangeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @JoinColumn(name = "document_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Block block;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlockOperationType operationType;
    
    private String oldContent;
    private String newContent;

    @Column(name = "old_parent_id")
    private Integer oldParentId;
    @Column(name = "new_parent_id")
    private Integer newParentId;
    
    private BigInteger oldPosition;
    private BigInteger newPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
    
    private Long versionNumber;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
}

