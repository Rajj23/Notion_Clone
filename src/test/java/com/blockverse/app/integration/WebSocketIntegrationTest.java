package com.blockverse.app.integration;

import com.blockverse.app.dto.activityFeed.CursorEvent;
import com.blockverse.app.dto.activityFeed.PresenceEvent;
import com.blockverse.app.dto.activityFeed.TypingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    private final String WEBSOCKET_URI = "ws://localhost:{port}/ws";

    @BeforeEach
    public void setup() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    public void testTypingEventBroadcast() throws ExecutionException, InterruptedException, TimeoutException {
        StompSession session = stompClient
                .connectAsync(WEBSOCKET_URI, new StompSessionHandlerAdapter() {}, port)
                .get(2, TimeUnit.SECONDS);

        CompletableFuture<TypingEvent> completableFuture = new CompletableFuture<>();

        session.subscribe("/topic/document/1/typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TypingEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completableFuture.complete((TypingEvent) payload);
            }
        });

        TypingEvent typingEvent = new TypingEvent(1, 10, 5, "start_typing");
        session.send("/app/document.typing", typingEvent);

        TypingEvent receivedEvent = completableFuture.get(5, TimeUnit.SECONDS);

        assertThat(receivedEvent.getDocumentId()).isEqualTo(1);
        assertThat(receivedEvent.getBlockId()).isEqualTo(10);
        assertThat(receivedEvent.getUserId()).isEqualTo(5);
        assertThat(receivedEvent.getAction()).isEqualTo("start_typing");
    }

    @Test
    public void testInvalidTypingEventIsNotBroadcast() throws ExecutionException, InterruptedException, TimeoutException {
        StompSession session = stompClient
                .connectAsync(WEBSOCKET_URI, new StompSessionHandlerAdapter() {}, port)
                .get(2, TimeUnit.SECONDS);

        CompletableFuture<TypingEvent> completableFuture = new CompletableFuture<>();

        session.subscribe("/topic/document/1/typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TypingEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completableFuture.complete((TypingEvent) payload);
            }
        });

        // Invalid event: negative documentId
        TypingEvent invalidEvent = new TypingEvent(-1, 10, 5, "start_typing");
        session.send("/app/document.typing", invalidEvent);

        // It should time out because validation should fail and message shouldn't be broadcast
        assertThrows(TimeoutException.class, () -> completableFuture.get(2, TimeUnit.SECONDS));
    }

    @Test
    public void testPresenceEventJoinAndLeave() throws ExecutionException, InterruptedException, TimeoutException {
        StompSession session = stompClient
                .connectAsync(WEBSOCKET_URI, new StompSessionHandlerAdapter() {}, port)
                .get(2, TimeUnit.SECONDS);

        // Test JOIN
        CompletableFuture<PresenceEvent> joinFuture = new CompletableFuture<>();
        StompSession.Subscription subscription = session.subscribe("/topic/document/2/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PresenceEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                PresenceEvent event = (PresenceEvent) payload;
                if ("join".equals(event.getAction())) {
                    joinFuture.complete(event);
                }
            }
        });

        PresenceEvent joinEvent = new PresenceEvent(2, 8, "Alice", "join");
        session.send("/app/document.join", joinEvent);

        PresenceEvent receivedJoin = joinFuture.get(5, TimeUnit.SECONDS);
        assertThat(receivedJoin.getAction()).isEqualTo("join");
        assertThat(receivedJoin.getUserName()).isEqualTo("Alice");

        subscription.unsubscribe();
        
        // Test LEAVE
        CompletableFuture<PresenceEvent> leaveFuture = new CompletableFuture<>();
        session.subscribe("/topic/document/2/presence", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return PresenceEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                PresenceEvent event = (PresenceEvent) payload;
                if ("leave".equals(event.getAction())) {
                    leaveFuture.complete(event);
                }
            }
        });

        PresenceEvent leaveEvent = new PresenceEvent(2, 8, "Alice", "leave");
        session.send("/app/document.leave", leaveEvent);

        PresenceEvent receivedLeave = leaveFuture.get(5, TimeUnit.SECONDS);
        assertThat(receivedLeave.getAction()).isEqualTo("leave");
        assertThat(receivedLeave.getUserName()).isEqualTo("Alice");
    }

    @Test
    public void testCursorEventBroadcast() throws ExecutionException, InterruptedException, TimeoutException {
        StompSession session = stompClient
                .connectAsync(WEBSOCKET_URI, new StompSessionHandlerAdapter() {}, port)
                .get(2, TimeUnit.SECONDS);

        CompletableFuture<CursorEvent> completableFuture = new CompletableFuture<>();

        session.subscribe("/topic/document/3/cursor", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return CursorEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                completableFuture.complete((CursorEvent) payload);
            }
        });

        CursorEvent cursorEvent = new CursorEvent(3, 20, 15, 42);
        session.send("/app/document.cursor", cursorEvent);

        CursorEvent receivedEvent = completableFuture.get(5, TimeUnit.SECONDS);

        assertThat(receivedEvent.getDocumentId()).isEqualTo(3);
        assertThat(receivedEvent.getBlockId()).isEqualTo(20);
        assertThat(receivedEvent.getUserId()).isEqualTo(15);
        assertThat(receivedEvent.getCursorPosition()).isEqualTo(42);
    }
}
