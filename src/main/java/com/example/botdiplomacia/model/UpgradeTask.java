package com.example.botdiplomacia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "upgrade_tasks")
public class UpgradeTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long gameAccountId;
    private String skillCode;
    private String costType;

    @Enumerated(EnumType.STRING)
    private UpgradeTaskStatus status = UpgradeTaskStatus.ACTIVE;

    private OffsetDateTime nextRunAt;
    private String lastError;
    private OffsetDateTime createdAt;

    public UpgradeTask() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameAccountId() {
        return gameAccountId;
    }

    public void setGameAccountId(Long gameAccountId) {
        this.gameAccountId = gameAccountId;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getCostType() {
        return costType;
    }

    public void setCostType(String costType) {
        this.costType = costType;
    }

    public UpgradeTaskStatus getStatus() {
        return status;
    }

    public void setStatus(UpgradeTaskStatus status) {
        this.status = status;
    }

    public OffsetDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(OffsetDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
