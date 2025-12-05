package org.pm.hamburgaiassistant.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pm.hamburgaiassistant.service.ConversationHistoryService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationContextTool {

    private final ConversationHistoryService conversationHistoryService;

    @Tool("Search through past conversation history to find relevant context. " +
            "Use this when the user references something from earlier in the conversation " +
            "or when you need context from previous messages.")
    public String searchConversationHistory(String query, String sessionId) {
        log.info("Searching conversation history for: {}", query);
        return conversationHistoryService.retrieveRelevantHistory(query, sessionId, 3);
    }
}