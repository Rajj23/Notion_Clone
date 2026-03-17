package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.PresenceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.Payload;

@Controller
@RequiredArgsConstructor
public class PresenceSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/document.join")
    public void joinDocument(@Payload @Valid PresenceEvent event){
        messagingTemplate.convertAndSend(
                "/topic/document/" + event.getDocumentId() + "/presence",
                event
        );
    }
    
    @MessageMapping("/document.leave")
    public void leaveDocument(@Payload @Valid PresenceEvent event){
        messagingTemplate.convertAndSend(
                "/topic/document/" + event.getDocumentId() + "/presence",
                event
        );
    }
    
}
