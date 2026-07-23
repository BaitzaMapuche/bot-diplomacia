package com.example.botdiplomacia.diplomacia;

public class UpgradeResult {
    private final boolean success;
    private final boolean authExpired;
    private final long cooldownMs;
    private final Integer currentLevel;
    private final Integer targetLevel;
    private final Integer cost;
    private final String errorMessage;

    private UpgradeResult(boolean success, boolean authExpired, long cooldownMs,
                           Integer currentLevel, Integer targetLevel, Integer cost, String errorMessage) {
        this.success = success;
        this.authExpired = authExpired;
        this.cooldownMs = cooldownMs;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.cost = cost;
        this.errorMessage = errorMessage;
    }

    public static UpgradeResult success(long cooldownMs, Integer currentLevel, Integer targetLevel, Integer cost) {
        return new UpgradeResult(true, false, cooldownMs, currentLevel, targetLevel, cost, null);
    }

    public static UpgradeResult authExpired() {
        return new UpgradeResult(false, true, 0, null, null, null, "Sesion expirada o invalida");
    }

    public static UpgradeResult failure(String errorMessage) {
        return new UpgradeResult(false, false, 0, null, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isAuthExpired() {
        return authExpired;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public Integer getTargetLevel() {
        return targetLevel;
    }

    public Integer getCost() {
        return cost;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
