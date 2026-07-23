package com.example.botdiplomacia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "authorized_users")
public class AuthorizedUser {
    @Id
    private Long telegramUserId;

    private String telegramUsername;
    private OffsetDateTime addedAt;

    public AuthorizedUser() {
    }

    public AuthorizedUser(Long telegramUserId, String telegramUsername) {
        this.telegramUserId = telegramUserId;
        this.telegramUsername = telegramUsername;
        this.addedAt = OffsetDateTime.now();
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(Long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(OffsetDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
