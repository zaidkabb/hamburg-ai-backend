package org.pm.hamburgaiassistant.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PgVectorConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${pgvector.document.table}")
    private String documentTable;

    @Value("${pgvector.conversation.table}")
    private String conversationTable;

    @Value("${pgvector.dimension}")
    private Integer dimension;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean(name = "documentEmbeddingStore")
    public EmbeddingStore<TextSegment> documentEmbeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(extractHost(datasourceUrl))
                .port(extractPort(datasourceUrl))
                .database(extractDatabase(datasourceUrl))
                .user(username)
                .password(password)
                .table(documentTable)
                .dimension(dimension)
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }

    @Bean(name = "conversationEmbeddingStore")
    public EmbeddingStore<TextSegment> conversationEmbeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(extractHost(datasourceUrl))
                .port(extractPort(datasourceUrl))
                .database(extractDatabase(datasourceUrl))
                .user(username)
                .password(password)
                .table(conversationTable)
                .dimension(dimension)
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }

    // Helper methods to parse connection string
    private String extractHost(String url) {
        // postgresql://host:port/database -> host
        String withoutProtocol = url.replace("postgresql://", "");
        String[] parts = withoutProtocol.split("@");
        if (parts.length > 1) {
            withoutProtocol = parts[1];
        }
        return withoutProtocol.split(":")[0].split("/")[0];
    }

    private Integer extractPort(String url) {
        // Default PostgreSQL port
        if (!url.contains(":")) return 5432;

        String withoutProtocol = url.replace("postgresql://", "");
        String[] parts = withoutProtocol.split("@");
        if (parts.length > 1) {
            withoutProtocol = parts[1];
        }

        if (withoutProtocol.contains(":") && withoutProtocol.contains("/")) {
            String portPart = withoutProtocol.split(":")[1].split("/")[0];
            return Integer.parseInt(portPart);
        }
        return 5432;
    }

    private String extractDatabase(String url) {
        // postgresql://host:port/database -> database
        String[] parts = url.split("/");
        String dbWithParams = parts[parts.length - 1];
        // Remove query parameters
        return dbWithParams.split("\\?")[0];
    }
}