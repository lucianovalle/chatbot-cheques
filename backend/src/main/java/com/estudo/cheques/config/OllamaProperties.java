package com.estudo.cheques.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.ollama")
public record OllamaProperties(String baseUrl, String modelName, String embeddingModel, int timeoutSeconds) {
}
