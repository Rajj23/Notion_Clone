package com.blockverse.app.service;

import com.blockverse.app.dto.document.DocumentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentSocketPublisher {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void broadcast(int documentId, DocumentEvent event){
        messagingTemplate.convertAndSend(
                "/topic/document/" + documentId,
                event
        );
    }
    
}
