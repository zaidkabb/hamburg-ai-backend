package org.pm.hamburgaiassistant.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.pm.hamburgaiassistant.tools.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLanguageModel chatLanguageModel;
    private final Map<Object, ChatMemory> chatMemoryStore;
    private final WeatherTool weatherTool;
    private final GooglePlacesTool googlePlacesTool;
    private final HamburgEventsTool eventbriteTool;
    private final HamburgEventsTool hamburgEventsTool;
    private final GoogleDirectionsTool googleDirectionsTool;
    private final KnowledgeBaseTool knowledgeBaseTool;

    public String chat(String sessionId, String userMessage) {
        // Get or create memory for this session
        ChatMemory chatMemory = chatMemoryStore.computeIfAbsent(
                sessionId,
                id -> MessageWindowChatMemory.withMaxMessages(10)
        );

        // Create assistant service with memory and all tools for this session
        AssistantService assistant = AiServices.builder(AssistantService.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(chatMemory)
                .tools(weatherTool, googlePlacesTool, eventbriteTool,
                        hamburgEventsTool, googleDirectionsTool, knowledgeBaseTool)
                .build();

        // Get response
        return assistant.chat(userMessage);
    }

    public void clearSession(String sessionId) {
        chatMemoryStore.remove(sessionId);
    }
}