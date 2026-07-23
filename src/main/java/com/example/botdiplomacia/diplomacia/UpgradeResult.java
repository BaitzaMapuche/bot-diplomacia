package com.example.botdiplomacia.diplomacia;

public class UpgradeResult {
    private final boolean success;
    private final boolean authExpired;
    private final long cooldownMs;
    private final String errorMessage;

    private UpgradeResult(boolean success, boolean authExpired, long cooldownMs, String errorMessage) {
        this.success = success;
        this.authExpired = authExpired;
        this.cooldownMs = cooldownMs;
        this.errorMessage = errorMessage;
    }

    public static UpgradeResult success(long cooldownMs) {
        return new UpgradeResult(true, false, cooldownMs, null);
    }

    public static UpgradeResult authExpired() {
        return new UpgradeResult(false, true, 0, "Sesion expirada o invalida");
    }

    public static UpgradeResult failure(String errorMessage) {
        return new UpgradeResult(false, false, 0, errorMessage);
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

    public String getErrorMessage() {
        return errorMessage;
    }
}
