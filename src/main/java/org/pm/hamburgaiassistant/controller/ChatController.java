package org.pm.hamburgaiassistant.controller;

import org.pm.hamburgaiassistant.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        // Use sessionId from request, or default to "default-session"
        String sessionId = request.sessionId() != null ? request.sessionId() : "default-session";
        String response = chatService.chat(sessionId, request.message());
        return new ChatResponse(response, sessionId);
    }

    @DeleteMapping("/session/{sessionId}")
    public void clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
    }

    // Inner classes for request/response
    public record ChatRequest(String message, String sessionId) {}
    public record ChatResponse(String response, String sessionId) {}
}