package org.pm.hamburgaiassistant.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingChatService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final Map<Object, ChatMemory> chatMemoryStore;

    public void streamChat(String sessionId, String userMessage, Consumer<String> onNext, Runnable onComplete) {
        try {
            // Get or create memory for this session
            ChatMemory chatMemory = chatMemoryStore.computeIfAbsent(
                    sessionId,
                    id -> MessageWindowChatMemory.withMaxMessages(10)
            );

            // Create streaming model
            StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
                    .baseUrl(apiUrl + "/v1")
                    .apiKey(apiKey)
                    .modelName("deepseek-chat")
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // Add user message to memory
            chatMemory.add(UserMessage.from(userMessage));

            // Stream response
            streamingModel.generate(chatMemory.messages(), new StreamingResponseHandler<AiMessage>() {
                private final StringBuilder fullResponse = new StringBuilder();

                @Override
                public void onNext(String token) {
                    fullResponse.append(token);
                    onNext.accept(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    // Add AI response to memory
                    chatMemory.add(response.content());
                    onComplete.run();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Error during streaming", error);
                    onNext.accept("\n\nError: " + error.getMessage());
                    onComplete.run();
                }
            });

        } catch (Exception e) {
            log.error("Error in streaming chat", e);
            onNext.accept("Error: " + e.getMessage());
            onComplete.run();
        }
    }
}