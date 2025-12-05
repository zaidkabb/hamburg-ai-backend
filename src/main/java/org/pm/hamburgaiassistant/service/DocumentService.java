package org.pm.hamburgaiassistant.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class DocumentService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public DocumentService(
            @Qualifier("documentEmbeddingStore") EmbeddingStore<TextSegment> embeddingStore,  // ADD @Qualifier
            EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void ingestDocument(Path filePath) {
        try {
            log.info("Ingesting document: {}", filePath);

            // Load document based on file type
            Document document;
            String fileName = filePath.getFileName().toString().toLowerCase();

            if (fileName.endsWith(".pdf")) {
                document = FileSystemDocumentLoader.loadDocument(filePath, new ApachePdfBoxDocumentParser());
            } else {
                document = FileSystemDocumentLoader.loadDocument(filePath, new TextDocumentParser());
            }

            // Split document into chunks
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
            List<TextSegment> segments = splitter.split(document);

            // Embed and store segments
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }

            log.info("Successfully ingested {} segments from {}", segments.size(), filePath.getFileName());
        } catch (Exception e) {
            log.error("Error ingesting document: {}", filePath, e);
            throw new RuntimeException("Failed to ingest document", e);
        }
    }

    public void ingestText(String text, String source) {
        try {
            log.info("Ingesting text from source: {}", source);

            // Create document from text
            Document document = Document.from(text);

            // Split into chunks
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
            List<TextSegment> segments = splitter.split(document);

            // Embed and store segments
            for (TextSegment segment : segments) {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            }

            log.info("Successfully ingested {} segments from {}", segments.size(), source);
        } catch (Exception e) {
            log.error("Error ingesting text from {}", source, e);
            throw new RuntimeException("Failed to ingest text", e);
        }
    }
}