package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.TypingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TypingSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TypingSocketController typingSocketController;

    private TypingEvent typingEvent;

    @BeforeEach
    void setUp() {
        typingEvent = new TypingEvent(1, 1, 1, "typing");
    }

    @Test
    void testTypingBroadcastsToCorrectTopic() {
        typingSocketController.typing(typingEvent);

        verify(messagingTemplate).convertAndSend(
                "/topic/document/1/typing",
                typingEvent
        );
    }
}
