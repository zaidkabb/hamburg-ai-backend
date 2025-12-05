package org.pm.hamburgaiassistant.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationHistoryService {

    private final EmbeddingStore<TextSegment> conversationStore;
    private final EmbeddingModel embeddingModel;

    public ConversationHistoryService(
            @Qualifier("conversationEmbeddingStore") EmbeddingStore<TextSegment> conversationStore,  // ADD @Qualifier
            EmbeddingModel embeddingModel) {
        this.conversationStore = conversationStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Store a conversation turn (user message + AI response)
     */
    public void storeConversation(String sessionId, String userMessage, String aiResponse) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

            // Combine user and AI message for context
            String conversationText = String.format(
                    "User: %s\nAssistant: %s",
                    userMessage,
                    aiResponse
            );

            // Create metadata
            Metadata metadata = new Metadata();
            metadata.put("sessionId", sessionId);
            metadata.put("timestamp", timestamp);
            metadata.put("userMessage", userMessage);
            metadata.put("aiResponse", aiResponse);

            // Create segment and embed
            TextSegment segment = TextSegment.from(conversationText, metadata);
            Embedding embedding = embeddingModel.embed(segment).content();

            // Store in database
            conversationStore.add(embedding, segment);

            log.info("Stored conversation for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error storing conversation", e);
        }
    }

    /**
     * Retrieve relevant conversation history based on current query
     */
    public String retrieveRelevantHistory(String query, String sessionId, int maxResults) {
        try {
            // Embed the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // Search for relevant conversations
            List<EmbeddingMatch<TextSegment>> matches = conversationStore.findRelevant(
                    queryEmbedding,
                    maxResults,
                    0.7 // minimum relevance score
            );

            if (matches.isEmpty()) {
                return "No relevant conversation history found.";
            }

            // Format the retrieved context
            String context = matches.stream()
                    .map(match -> {
                        TextSegment segment = match.embedded();
                        Metadata metadata = segment.metadata();
                        return String.format(
                                "[Previous conversation from %s]\n%s",
                                metadata.getString("timestamp"),
                                segment.text()
                        );
                    })
                    .collect(Collectors.joining("\n\n"));

            log.info("Retrieved {} relevant conversation(s) for query", matches.size());
            return context;
        } catch (Exception e) {
            log.error("Error retrieving conversation history", e);
            return "Error retrieving conversation history.";
        }
    }
}