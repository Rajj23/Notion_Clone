package com.blockverse.app.dto.activityFeed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TypingEvent {
    @Positive(message = "Document ID must be positive")
    private int documentId;
    
    @Positive(message = "Block ID must be positive")
    private int blockId;
    
    @Positive(message = "User ID must be positive")
    private int userId;
    
    @NotBlank(message = "Action is required")
    private String action;
}
