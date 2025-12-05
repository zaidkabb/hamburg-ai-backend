package org.pm.hamburgaiassistant.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pm.hamburgaiassistant.service.StreamingChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final StreamingChatService streamingChatService;
    private final Gson gson = new Gson();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());

        // Send welcome message
        JsonObject welcome = new JsonObject();
        welcome.addProperty("type", "connected");
        welcome.addProperty("message", "Connected to Hamburg AI Assistant");
        session.sendMessage(new TextMessage(gson.toJson(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("Received message: {}", payload);

            // Parse incoming message
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            String userMessage = json.get("message").getAsString();
            String sessionId = json.has("sessionId")
                    ? json.get("sessionId").getAsString()
                    : session.getId();

            // Send streaming response
            streamingChatService.streamChat(
                    sessionId,
                    userMessage,
                    // On each token
                    token -> {
                        try {
                            JsonObject response = new JsonObject();
                            response.addProperty("type", "token");
                            response.addProperty("content", token);
                            session.sendMessage(new TextMessage(gson.toJson(response)));
                        } catch (IOException e) {
                            log.error("Error sending token", e);
                        }
                    },
                    // On complete
                    () -> {
                        try {
                            JsonObject complete = new JsonObject();
                            complete.addProperty("type", "complete");
                            session.sendMessage(new TextMessage(gson.toJson(complete)));
                        } catch (IOException e) {
                            log.error("Error sending completion", e);
                        }
                    }
            );

        } catch (Exception e) {
            log.error("Error handling message", e);
            JsonObject error = new JsonObject();
            error.addProperty("type", "error");
            error.addProperty("message", "Error: " + e.getMessage());
            session.sendMessage(new TextMessage(gson.toJson(error)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket connection closed: {} - {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }
}