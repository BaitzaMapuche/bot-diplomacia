package com.example.botdiplomacia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;
    private String apiUrl;
    private Long ownerId;
    private String webhookSecret;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
