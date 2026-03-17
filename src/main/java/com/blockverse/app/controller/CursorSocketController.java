package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.CursorEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.Payload;

@Controller
@RequiredArgsConstructor
public class CursorSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/document.cursor")
    public void cursor(@Payload @Valid CursorEvent event){
        messagingTemplate.convertAndSend(
                "/topic/document/" + event.getDocumentId() + "/cursor",
                event
        );
    }
    
}
