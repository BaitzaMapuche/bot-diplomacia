package com.example.botdiplomacia.service;

import com.example.botdiplomacia.config.TelegramProperties;
import com.example.botdiplomacia.diplomacia.SkillCatalog;
import com.example.botdiplomacia.model.AuthorizedUser;
import com.example.botdiplomacia.model.GameAccount;
import com.example.botdiplomacia.model.TelegramChatMessage;
import com.example.botdiplomacia.model.UpgradeTask;
import com.example.botdiplomacia.model.UpgradeTaskStatus;
import com.example.botdiplomacia.repository.AuthorizedUserRepository;
import com.example.botdiplomacia.repository.GameAccountRepository;
import com.example.botdiplomacia.repository.TelegramChatMessageRepository;
import com.example.botdiplomacia.repository.UpgradeTaskRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class TelegramService {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String TOKEN_HELP =
            "   Como conseguirlo: entra a diplomacia.com.tr ya logueado, abre las herramientas de desarrollador (F12), "
                    + "pestana Network, actualiza la pagina o haz cualquier accion, busca cualquier peticion al sitio y copia "
                    + "el valor que aparece en el header 'Authorization' despues de la palabra 'Bearer '.";

    private final TelegramProperties telegramProperties;
    private final TelegramChatMessageRepository chatMessageRepository;
    private final AuthorizedUserRepository authorizedUserRepository;
    private final GameAccountRepository gameAccountRepository;
    private final UpgradeTaskRepository upgradeTaskRepository;
    private final TelegramNotifier notifier;

    public TelegramService(TelegramProperties telegramProperties,
                            TelegramChatMessageRepository chatMessageRepository,
                            AuthorizedUserRepository authorizedUserRepository,
                            GameAccountRepository gameAccountRepository,
                            UpgradeTaskRepository upgradeTaskRepository,
                            TelegramNotifier notifier) {
        this.telegramProperties = telegramProperties;
        this.chatMessageRepository = chatMessageRepository;
        this.authorizedUserRepository = authorizedUserRepository;
        this.gameAccountRepository = gameAccountRepository;
        this.upgradeTaskRepository = upgradeTaskRepository;
        this.notifier = notifier;
    }

    public void processUpdate(Update update) {
        if (update == null || !update.hasMessage() || update.getMessage().getChat() == null) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        Integer messageId = update.getMessage().getMessageId();
        String text = update.getMessage().getText();
        User from = update.getMessage().getFrom();
        String username = from != null ? from.getUserName() : null;

        boolean authorized = isAuthorized(chatId);
        logIncoming(chatId, from, authorized, text);

        chatMessageRepository.save(buildLogEntry(chatId, messageId, username, text));

        if (text == null || text.isBlank()) {
            return;
        }

        if (!authorized) {
            notifier.sendMessage(chatId, "No estas autorizado a usar este bot.");
            return;
        }

        String reply = route(chatId, messageId, text.strip());
        if (reply != null) {
            notifier.sendMessage(chatId, reply);
        }
    }

    private void logIncoming(Long chatId, User from, boolean authorized, String text) {
        String fullName = from != null
                ? ((from.getFirstName() != null ? from.getFirstName() : "") + " " + (from.getLastName() != null ? from.getLastName() : "")).strip()
                : "desconocido";
        String username = from != null && from.getUserName() != null ? "@" + from.getUserName() : "(sin username)";
        String safeText = redact(text);

        if (authorized) {
            log.info("Mensaje de usuario AUTORIZADO - userId={} username={} nombre=\"{}\" texto=\"{}\"",
                    chatId, username, fullName, safeText);
        } else {
            log.warn("Mensaje de usuario NO AUTORIZADO - userId={} username={} nombre=\"{}\" texto=\"{}\"",
                    chatId, username, fullName, safeText);
        }
    }

    private String redact(String text) {
        if (text != null && text.strip().toLowerCase().startsWith("/token")) {
            return "/token [REDACTED]";
        }
        return text;
    }

    private TelegramChatMessage buildLogEntry(Long chatId, Integer messageId, String username, String text) {
        TelegramChatMessage entry = new TelegramChatMessage();
        entry.setChatId(chatId);
        entry.setMessageId(messageId != null ? messageId.longValue() : null);
        entry.setUserName(username);
        entry.setText(redact(text));
        entry.setReceivedAt(OffsetDateTime.now());
        return entry;
    }

    private boolean isOwner(Long chatId) {
        return telegramProperties.getOwnerId() != null && telegramProperties.getOwnerId().equals(chatId);
    }

    private boolean isAuthorized(Long chatId) {
        return isOwner(chatId) || authorizedUserRepository.existsById(chatId);
    }

    private String route(Long chatId, Integer messageId, String text) {
        String[] parts = text.split("\\s+");
        String command = parts[0].toLowerCase();

        return switch (command) {
            case "/start" -> "Bienvenido a tu bot de diplomacia. Usa /help para ver los comandos disponibles.";
            case "/help" -> helpText();
            case "/autorizar" -> handleAuthorize(chatId, parts);
            case "/token" -> handleToken(chatId, messageId, text, parts[0]);
            case "/autosubir" -> handleAutoUpgrade(chatId, parts);
            case "/parar" -> handleStop(chatId);
            case "/estado" -> handleStatus(chatId);
            default -> "Comando no reconocido. Usa /help para ver los comandos disponibles.";
        };
    }

    private String helpText() {
        StringBuilder sb = new StringBuilder("Comandos disponibles:\n");
        sb.append("/token <token> - guarda o actualiza tu sesion de diplomacia.com.tr (borro tu mensaje despues)\n");
        sb.append(TOKEN_HELP).append("\n");
        sb.append("/autosubir <habilidad> <recurso> - activa la subida automatica de una estadistica\n");
        sb.append("   Habilidades: ").append(SkillCatalog.availableSkillsText()).append("\n");
        sb.append("   Recursos: ").append(SkillCatalog.availableResourcesText()).append("\n");
        sb.append("/parar - detiene todas tus subidas automaticas\n");
        sb.append("/estado - muestra tus tareas y cuando corren de nuevo\n");
        return sb.toString();
    }

    private String handleAuthorize(Long chatId, String[] parts) {
        if (!isOwner(chatId)) {
            return "No tienes permiso para usar este comando.";
        }
        if (parts.length < 2) {
            return "Uso: /autorizar <telegram_id>";
        }
        try {
            Long targetId = Long.parseLong(parts[1]);
            if (authorizedUserRepository.existsById(targetId)) {
                return "Ese usuario ya estaba autorizado.";
            }
            authorizedUserRepository.save(new AuthorizedUser(targetId, null));
            return "Usuario " + targetId + " autorizado.";
        } catch (NumberFormatException e) {
            return "El telegram_id debe ser un numero.";
        }
    }

    private String handleToken(Long chatId, Integer messageId, String text, String commandWord) {
        String token = text.substring(commandWord.length()).strip();
        if (token.isEmpty()) {
            return "Uso: /token <tu_token>\n" + TOKEN_HELP;
        }

        GameAccount account = gameAccountRepository.findByTelegramUserId(chatId).orElseGet(GameAccount::new);
        account.setTelegramUserId(chatId);
        account.setSessionToken(token);
        account.setTokenUpdatedAt(OffsetDateTime.now());
        account.setActive(true);
        gameAccountRepository.save(account);

        List<UpgradeTask> tasks = upgradeTaskRepository.findByGameAccountId(account.getId());
        for (UpgradeTask task : tasks) {
            if (task.getStatus() == UpgradeTaskStatus.ERROR) {
                task.setStatus(UpgradeTaskStatus.ACTIVE);
                task.setNextRunAt(OffsetDateTime.now());
                task.setLastError(null);
                upgradeTaskRepository.save(task);
            }
        }

        if (messageId != null) {
            notifier.deleteMessage(chatId, messageId);
        }
        return "Token guardado. Borre tu mensaje por seguridad. Tareas en error se reactivaron.";
    }

    private String handleAutoUpgrade(Long chatId, String[] parts) {
        if (parts.length < 3) {
            return "Uso: /autosubir <habilidad> <recurso>\nHabilidades: " + SkillCatalog.availableSkillsText()
                    + "\nRecursos: " + SkillCatalog.availableResourcesText();
        }
        String skillCode = SkillCatalog.resolveSkillCode(parts[1]);
        if (skillCode == null) {
            return "No reconozco la habilidad '" + parts[1] + "'. Opciones: " + SkillCatalog.availableSkillsText();
        }
        String costType = SkillCatalog.resolveResourceCode(parts[2]);
        if (costType == null) {
            return "No reconozco el recurso '" + parts[2] + "'. Opciones: " + SkillCatalog.availableResourcesText();
        }

        Optional<GameAccount> accountOpt = gameAccountRepository.findByTelegramUserId(chatId);
        if (accountOpt.isEmpty()) {
            return "Primero manda /token <tu_token> para vincular tu cuenta del juego.";
        }
        GameAccount account = accountOpt.get();
        if (!account.isActive()) {
            return "Tu token esta marcado como invalido. Manda /token <nuevo_token> primero.";
        }

        UpgradeTask task = upgradeTaskRepository.findByGameAccountIdAndSkillCode(account.getId(), skillCode)
                .orElseGet(UpgradeTask::new);
        boolean isNew = task.getId() == null;
        task.setGameAccountId(account.getId());
        task.setSkillCode(skillCode);
        task.setCostType(costType);
        task.setStatus(UpgradeTaskStatus.ACTIVE);
        task.setNextRunAt(OffsetDateTime.now());
        task.setLastError(null);
        if (isNew) {
            task.setCreatedAt(OffsetDateTime.now());
        }
        upgradeTaskRepository.save(task);

        String reply = "Subida automatica activada para " + SkillCatalog.skillDisplayName(skillCode) + " pagando con "
                + SkillCatalog.resourceDisplayName(costType) + ".";
        if (account.isBusy(OffsetDateTime.now())) {
            reply += "\nOjo: ahora mismo el juego ya tiene otra subida en curso (" + SkillCatalog.skillDisplayName(account.getBusySkillCode())
                    + "), asi que esta empezara automaticamente cuando esa termine, aprox a las "
                    + account.getBusyUntil().format(TIME_FORMAT) + ".";
        }
        return reply;
    }

    private String handleStop(Long chatId) {
        Optional<GameAccount> accountOpt = gameAccountRepository.findByTelegramUserId(chatId);
        if (accountOpt.isEmpty()) {
            return "No hay nada para detener.";
        }
        GameAccount account = accountOpt.get();
        List<UpgradeTask> tasks = upgradeTaskRepository.findByGameAccountId(account.getId());
        List<UpgradeTask> activeTasks = tasks.stream().filter(t -> t.getStatus() == UpgradeTaskStatus.ACTIVE).toList();

        if (activeTasks.isEmpty()) {
            return "No hay nada para detener.";
        }

        activeTasks.forEach(t -> t.setStatus(UpgradeTaskStatus.PAUSED));
        upgradeTaskRepository.saveAll(activeTasks);

        String reply = "Se detuvo la subida automatica.";
        if (account.isBusy(OffsetDateTime.now())) {
            reply += "\nOjo: el juego ya tenia una subida en curso (" + SkillCatalog.skillDisplayName(account.getBusySkillCode())
                    + "), eso no se puede cancelar — solo evito que el bot la vuelva a repetir cuando termine.";
        }
        return reply;
    }

    private String handleStatus(Long chatId) {
        Optional<GameAccount> accountOpt = gameAccountRepository.findByTelegramUserId(chatId);
        if (accountOpt.isEmpty()) {
            return "No tienes ninguna cuenta vinculada. Manda /token <tu_token> para empezar.";
        }
        GameAccount account = accountOpt.get();
        List<UpgradeTask> tasks = upgradeTaskRepository.findByGameAccountId(account.getId());
        if (tasks.isEmpty()) {
            return "No tienes tareas configuradas. Usa /autosubir <habilidad> <recurso>.";
        }

        StringBuilder sb = new StringBuilder();
        if (account.isBusy(OffsetDateTime.now())) {
            sb.append("Cuenta ocupada: subiendo ").append(SkillCatalog.skillDisplayName(account.getBusySkillCode()))
                    .append(" hasta las ").append(account.getBusyUntil().format(TIME_FORMAT)).append(".\n\n");
        }
        sb.append("Tus tareas:\n");
        for (UpgradeTask task : tasks) {
            sb.append("- ").append(SkillCatalog.skillDisplayName(task.getSkillCode())).append(" (")
                    .append(SkillCatalog.resourceDisplayName(task.getCostType())).append("): ")
                    .append(task.getStatus());
            if (task.getStatus() == UpgradeTaskStatus.ACTIVE && task.getNextRunAt() != null) {
                sb.append(", proximo intento ").append(task.getNextRunAt().format(TIME_FORMAT));
            }
            if (task.getStatus() == UpgradeTaskStatus.ERROR && task.getLastError() != null) {
                sb.append(" (").append(task.getLastError()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
