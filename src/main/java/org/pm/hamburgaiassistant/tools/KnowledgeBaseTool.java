package org.pm.hamburgaiassistant.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KnowledgeBaseTool {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    // Use constructor injection with @Qualifier
    public KnowledgeBaseTool(
            @Qualifier("documentEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    @Tool("Search the knowledge base for information about Hamburg. " +
            "Use this when the user asks about Hamburg attractions, history, or local information.")
    public String searchKnowledgeBase(String query) {
        try {
            log.info("Searching knowledge base for: {}", query);

            // Embed the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // Search for relevant segments
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                    queryEmbedding,
                    5,  // max results
                    0.6 // minimum score
            );

            if (matches.isEmpty()) {
                return "No relevant information found in the knowledge base about: " + query;
            }

            // Format results
            String results = matches.stream()
                    .map(match -> String.format(
                            "Relevance: %.2f - %s",
                            match.score(),
                            match.embedded().text()
                    ))
                    .collect(Collectors.joining("\n\n"));

            log.info("Found {} relevant documents", matches.size());
            return results;

        } catch (Exception e) {
            log.error("Error searching knowledge base", e);
            return "Error searching knowledge base: " + e.getMessage();
        }
    }
}