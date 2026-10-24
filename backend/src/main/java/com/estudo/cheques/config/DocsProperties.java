package com.estudo.cheques.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.docs")
public record DocsProperties(String path) {
}
