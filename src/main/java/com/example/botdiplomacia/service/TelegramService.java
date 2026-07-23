package com.example.botdiplomacia.service;

import com.example.botdiplomacia.config.TelegramProperties;
import com.example.botdiplomacia.model.TelegramChatMessage;
import com.example.botdiplomacia.repository.TelegramChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    private final TelegramProperties telegramProperties;
    private final TelegramChatMessageRepository repository;
    private final RestTemplate restTemplate;

    public TelegramService(TelegramProperties telegramProperties,
                           TelegramChatMessageRepository repository,
                           RestTemplate restTemplate) {
        this.telegramProperties = telegramProperties;
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    public void processUpdate(Update update) {
        if (update == null || !update.hasMessage() || update.getMessage().getChat() == null) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        Long messageId = update.getMessage().getMessageId().longValue();
        String text = update.getMessage().getText();
        String username = update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUserName() : null;

        TelegramChatMessage chatMessage = new TelegramChatMessage();
        chatMessage.setChatId(chatId);
        chatMessage.setMessageId(messageId);
        chatMessage.setText(text);
        chatMessage.setUserName(username);
        chatMessage.setReceivedAt(OffsetDateTime.now());
        repository.save(chatMessage);

        String replyText = buildReplyText(text, username);
        sendTelegramMessage(chatId, replyText);
    }

    private String buildReplyText(String incomingText, String username) {
        if (incomingText == null || incomingText.isBlank()) {
            return "Recibí tu mensaje, pero no pude leerlo. Intenta enviar texto simple.";
        }

        String lower = incomingText.strip().toLowerCase();
        if (lower.equals("/start")) {
            return "¡Bienvenido! Soy tu bot de diplomacia. Envía cualquier mensaje y lo responderé.";
        }

        if (lower.equals("/help")) {
            return "Comandos disponibles:\n/start - iniciar\n/help - mostrar ayuda\n/echo - repite tu texto";
        }

        if (lower.startsWith("/echo")) {
            String echoText = incomingText.length() > 5 ? incomingText.substring(5).strip() : "Nada para repetir.";
            return "Echo: " + echoText;
        }

        return "Hola" + (username != null ? " @" + username : "") + ", recibí: " + incomingText;
    }

    private void sendTelegramMessage(Long chatId, String text) {
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
}
