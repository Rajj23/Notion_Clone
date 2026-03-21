package com.blockverse.app.entity;

import com.blockverse.app.enums.NotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private Integer userId;
    private String message;
    
    private boolean read = false;
    
    private NotificationType type;
    
    private Integer referencedId; 
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
