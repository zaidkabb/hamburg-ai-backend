package org.pm.hamburgaiassistant.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.pm.hamburgaiassistant.tools.*;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatService {

    private final ChatLanguageModel chatModel;
    private final WeatherTool weatherTool;
    private final GooglePlacesTool googlePlacesTool;
    private final GoogleDirectionsTool googleDirectionsTool;  // FIXED!
    private final HamburgEventsTool eventbriteTool;
    private final HamburgEventsTool hamburgEventsTool;

    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    private final Map<String, Assistant> sessionAssistants = new ConcurrentHashMap<>();

    public ChatService(
            ChatLanguageModel chatModel,
            WeatherTool weatherTool,
            GooglePlacesTool googlePlacesTool,
            GoogleDirectionsTool googleDirectionsTool,  // FIXED!
            HamburgEventsTool eventbriteTool,
            HamburgEventsTool hamburgEventsTool
    ) {
        this.chatModel = chatModel;
        this.weatherTool = weatherTool;
        this.googlePlacesTool = googlePlacesTool;
        this.googleDirectionsTool = googleDirectionsTool;  // FIXED!
        this.eventbriteTool = eventbriteTool;
        this.hamburgEventsTool = hamburgEventsTool;
    }

    public String chat(String sessionId, String userMessage) {
        log.info("Processing message for session {}: {}", sessionId, userMessage);

        Assistant assistant = sessionAssistants.computeIfAbsent(sessionId, id -> {
            ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
            sessionMemories.put(id, memory);

            List<Object> tools = Arrays.asList(
                    weatherTool,
                    googlePlacesTool,
                    googleDirectionsTool,  // FIXED!
                    eventbriteTool,
                    hamburgEventsTool
            );

            return AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .chatMemory(memory)
                    .tools(tools)
                    .build();
        });

        String response = assistant.chat(userMessage);
        log.info("Generated response for session {}: {}", sessionId, response);

        return response;
    }

    interface Assistant {
        String chat(String userMessage);
    }

    // New method: clear stored memory and assistant for a session
    public void clearSession(String sessionId) {
        if (sessionId == null) return;
        sessionAssistants.remove(sessionId);
        sessionMemories.remove(sessionId);
        log.info("Cleared session {}", sessionId);
    }
}