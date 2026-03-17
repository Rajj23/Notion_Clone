package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.CursorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CursorSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CursorSocketController cursorSocketController;

    private CursorEvent cursorEvent;

    @BeforeEach
    void setUp() {
        cursorEvent = new CursorEvent(1, 1, 1, 15);
    }

    @Test
    void testCursorBroadcastsToCorrectTopic() {
        cursorSocketController.cursor(cursorEvent);

        verify(messagingTemplate).convertAndSend(
                "/topic/document/1/cursor",
                cursorEvent
        );
    }
}
