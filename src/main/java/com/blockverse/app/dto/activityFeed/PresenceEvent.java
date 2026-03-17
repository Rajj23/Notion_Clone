package com.blockverse.app.dto.activityFeed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PresenceEvent {
    
    @Positive(message = "Document ID must be positive")
    private int documentId;
    
    @Positive(message = "User ID must be positive")
    private int userId;
    
    @NotBlank(message = "User name is required")
    private String userName;
    
    @NotBlank(message = "Action is required")
    private String action;
}
