package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.TypingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.Payload;

@Controller
@RequiredArgsConstructor
public class TypingSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/document.typing")
    public void typing(@Payload @Valid TypingEvent event){
        messagingTemplate.convertAndSend(
                "/topic/document/" + event.getDocumentId() + "/typing",
                event
        );
    }
    
}
