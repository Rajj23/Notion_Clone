package com.blockverse.app.dto.activityFeed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CursorEvent {
    @Positive(message = "Document ID must be positive")
    private int documentId;
    
    @Positive(message = "Block ID must be positive")
    private int blockId;
    
    @Positive(message = "User ID must be positive")
    private int userId;
    
    @PositiveOrZero(message = "Cursor position cannot be negative")
    private int cursorPosition;
}
