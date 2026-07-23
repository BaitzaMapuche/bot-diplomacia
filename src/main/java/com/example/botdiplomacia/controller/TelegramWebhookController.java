package com.example.botdiplomacia.controller;

import com.example.botdiplomacia.config.TelegramProperties;
import com.example.botdiplomacia.service.TelegramService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("/webhook")
public class TelegramWebhookController {
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramService telegramService;
    private final TelegramProperties telegramProperties;

    public TelegramWebhookController(TelegramService telegramService, TelegramProperties telegramProperties) {
        this.telegramService = telegramService;
        this.telegramProperties = telegramProperties;
    }

    @PostMapping
    public ResponseEntity<String> receiveUpdate(
            @RequestHeader(value = SECRET_HEADER, required = false) String secretHeader,
            @RequestBody Update update) {
        if (!isFromTelegram(secretHeader)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        telegramService.processUpdate(update);
        return ResponseEntity.ok("OK");
    }

    private boolean isFromTelegram(String secretHeader) {
        String expected = telegramProperties.getWebhookSecret();
        if (expected == null || expected.isBlank()) {
            // Sin secreto configurado no podemos verificar el origen; mejor rechazar que aceptar a ciegas.
            return false;
        }
        if (secretHeader == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                secretHeader.getBytes(StandardCharsets.UTF_8));
    }
}
