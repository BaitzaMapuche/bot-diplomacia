package com.example.botdiplomacia.service;

import com.example.botdiplomacia.config.TelegramProperties;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramNotifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final TelegramProperties telegramProperties;
    private final RestTemplate restTemplate;

    public TelegramNotifier(TelegramProperties telegramProperties, RestTemplate restTemplate) {
        this.telegramProperties = telegramProperties;
        this.restTemplate = restTemplate;
    }

    public void sendMessage(Long chatId, String text) {
        try {
            String url = String.format("%s/bot%s/sendMessage", telegramProperties.getApiUrl(), telegramProperties.getBotToken());
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForLocation(new URI(url), request);
        } catch (Exception e) {
            log.error("Error enviando mensaje a Telegram (chatId={})", chatId, e);
        }
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        try {
            String url = String.format("%s/bot%s/deleteMessage", telegramProperties.getApiUrl(), telegramProperties.getBotToken());
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("message_id", messageId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForLocation(new URI(url), request);
        } catch (Exception e) {
            log.warn("No se pudo borrar el mensaje {} en chat {}", messageId, chatId, e);
        }
    }
}
