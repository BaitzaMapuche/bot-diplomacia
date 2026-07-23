package com.example.botdiplomacia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "diplomacia")
public class DiplomaciaProperties {
    private String apiBaseUrl = "https://diplomacia.com.tr";
    private String upgradePath = "/api/players/skills/upgrade";

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getUpgradePath() {
        return upgradePath;
    }

    public void setUpgradePath(String upgradePath) {
        this.upgradePath = upgradePath;
    }
}
