package org.pm.hamburgaiassistant.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pm.hamburgaiassistant.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading document: {}", file.getOriginalFilename());

            // Save file temporarily
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Ingest document
            documentService.ingestDocument(tempFile);

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(new UploadResponse(
                    "Document uploaded and indexed successfully: " + file.getOriginalFilename(),
                    true
            ));
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.internalServerError().body(new UploadResponse(
                    "Error uploading document: " + e.getMessage(),
                    false
            ));
        }
    }

    @PostMapping("/upload-text")
    public ResponseEntity<UploadResponse> uploadText(@RequestBody TextUploadRequest request) {
        try {
            log.info("Uploading text from source: {}", request.source());

            documentService.ingestText(request.text(), request.source());

            return ResponseEntity.ok(new UploadResponse(
                    "Text uploaded and indexed successfully from: " + request.source(),
                    true
            ));
        } catch (Exception e) {
            log.error("Error uploading text", e);
            return ResponseEntity.internalServerError().body(new UploadResponse(
                    "Error uploading text: " + e.getMessage(),
                    false
            ));
        }
    }

    public record TextUploadRequest(String text, String source) {}
    public record UploadResponse(String message, boolean success) {}
}