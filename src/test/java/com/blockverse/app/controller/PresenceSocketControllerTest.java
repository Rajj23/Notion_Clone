package com.blockverse.app.controller;

import com.blockverse.app.dto.activityFeed.PresenceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PresenceSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PresenceSocketController presenceSocketController;

    private PresenceEvent presenceEvent;

    @BeforeEach
    void setUp() {
        presenceEvent = new PresenceEvent(1, 1, "John Doe", "join");
    }

    @Test
    void testJoinDocumentBroadcastsToCorrectTopic() {
        presenceSocketController.joinDocument(presenceEvent);

        verify(messagingTemplate).convertAndSend(
                "/topic/document/1/presence",
                presenceEvent
        );
    }

    @Test
    void testLeaveDocumentBroadcastsToCorrectTopic() {
        presenceEvent.setAction("leave");
        presenceSocketController.leaveDocument(presenceEvent);

        verify(messagingTemplate).convertAndSend(
                "/topic/document/1/presence",
                presenceEvent
        );
    }
}
