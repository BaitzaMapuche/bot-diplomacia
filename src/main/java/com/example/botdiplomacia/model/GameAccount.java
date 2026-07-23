package com.example.botdiplomacia.model;

import com.example.botdiplomacia.crypto.TokenEncryptionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "game_accounts")
public class GameAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long telegramUserId;

    @Convert(converter = TokenEncryptionConverter.class)
    @Column(length = 2048)
    private String sessionToken;

    private OffsetDateTime tokenUpdatedAt;
    private boolean active = true;

    /** Hasta cuando hay una subida en curso en el juego (solo puede haber una por cuenta a la vez). */
    private OffsetDateTime busyUntil;
    private String busySkillCode;

    /** Null significa "no configurado todavia" y se trata como true (encendido por defecto). */
    private Boolean notifyOnStart;

    public GameAccount() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public OffsetDateTime getTokenUpdatedAt() {
        return tokenUpdatedAt;
    }

    public void setTokenUpdatedAt(OffsetDateTime tokenUpdatedAt) {
        this.tokenUpdatedAt = tokenUpdatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getBusyUntil() {
        return busyUntil;
    }

    public void setBusyUntil(OffsetDateTime busyUntil) {
        this.busyUntil = busyUntil;
    }

    public String getBusySkillCode() {
        return busySkillCode;
    }

    public void setBusySkillCode(String busySkillCode) {
        this.busySkillCode = busySkillCode;
    }

    public boolean isBusy(OffsetDateTime now) {
        return busyUntil != null && busyUntil.isAfter(now);
    }

    public boolean isNotifyOnStart() {
        return notifyOnStart == null || notifyOnStart;
    }

    public void setNotifyOnStart(boolean notifyOnStart) {
        this.notifyOnStart = notifyOnStart;
    }
}
