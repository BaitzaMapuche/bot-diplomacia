package com.example.botdiplomacia.diplomacia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpgradeResponse {
    private boolean success;

    @JsonProperty("cooldown_ms")
    private long cooldownMs;

    private String error;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
