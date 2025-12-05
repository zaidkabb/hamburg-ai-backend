package org.pm.hamburgaiassistant.service;

import dev.langchain4j.service.SystemMessage;

public interface AssistantService {

    @SystemMessage("You are a helpful AI assistant for Hamburg, Germany. " +
            "You help people with information about Hamburg including restaurants, events, " +
            "tourist attractions, and general information about the city.")
    String chat(String userMessage);
}