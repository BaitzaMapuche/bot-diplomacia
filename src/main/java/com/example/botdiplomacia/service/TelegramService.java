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
            "   Como conseguirlo:\n"
                    + "   1. Entra a diplomacia.com.tr ya logueado con tu cuenta.\n"
                    + "   2. Abre las herramientas de desarrollador con la tecla F12.\n"
                    + "   3. Arriba en esa ventana, haz click en la pestana 'Network'.\n"
                    + "   4. Con esa pestana abierta, recarga la pagina (F5) o haz click en cualquier boton del juego.\n"
                    + "   5. Va a aparecer una lista con varias filas en la columna 'Name'. Haz click en cualquiera de esas filas.\n"
                    + "   6. Se abre un panel a la derecha (o abajo). Ahi busca la seccion 'Headers'.\n"
                    + "   7. Dentro de 'Headers', baja hasta 'Request Headers' y busca la linea 'Authorization'.\n"
                    + "   8. Esa linea dice 'Bearer ' seguido de un texto largo. Copia solo ese texto largo (sin la palabra 'Bearer').";

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
            case "/token" -> handleToken(chatId, messageId, parts);
            case "/autosubir" -> handleAutoUpgrade(chatId, parts);
            case "/parar" -> handleStop(chatId, parts);
            case "/borrar" -> handleForget(chatId, parts);
            case "/estado" -> handleStatus(chatId);
            case "/notificaciones" -> handleNotifications(chatId, parts);
            default -> "Comando no reconocido. Usa /help para ver los comandos disponibles.";
        };
    }

    private String helpText() {
        StringBuilder sb = new StringBuilder("Comandos disponibles:\n");
        sb.append("Puedes vincular varias cuentas del juego. Cada una necesita un nombre corto sin espacios (ej. principal, alt1).\n\n");
        sb.append("/token <token> <cuenta> - vincula o actualiza el token de esa cuenta\n");
        sb.append(TOKEN_HELP).append("\n");
        sb.append("/autosubir <habilidad> <recurso> <cuenta> - activa la subida automatica en esa cuenta\n");
        sb.append("   Habilidades: ").append(SkillCatalog.availableSkillsText()).append("\n");
        sb.append("   Recursos: ").append(SkillCatalog.availableResourcesText()).append("\n");
        sb.append("/parar <cuenta|todas> - detiene la subida automatica de esa cuenta (o de todas)\n");
        sb.append("/borrar <cuenta|todas> - elimina el token y las tareas de esa cuenta (no se puede deshacer)\n");
        sb.append("/estado - muestra el estado de todas tus cuentas\n");
        sb.append("/notificaciones <on|off> <cuenta> - activa o desactiva el aviso cada vez que arranca una subida en esa cuenta (viene en on por defecto)\n");
        return sb.toString();
    }

    private String handleNotifications(Long chatId, String[] parts) {
        if (parts.length < 3 || !(parts[1].equalsIgnoreCase("on") || parts[1].equalsIgnoreCase("off"))) {
            return "Uso: /notificaciones <on|off> <cuenta>";
        }
        String accountName = parts[2];
        Optional<GameAccount> accountOpt = gameAccountRepository.findByTelegramUserIdAndNameIgnoreCase(chatId, accountName);
        if (accountOpt.isEmpty()) {
            return "No tienes una cuenta llamada '" + accountName + "'.";
        }

        GameAccount account = accountOpt.get();
        boolean enable = parts[1].equalsIgnoreCase("on");
        account.setNotifyOnStart(enable);
        gameAccountRepository.save(account);

        return enable
                ? "Listo, te voy a avisar cada vez que arranque una subida en '" + accountName + "'."
                : "Listo, ya no te aviso cuando arranca una subida en '" + accountName + "' (los errores y el token vencido si te los sigo avisando).";
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

    private String handleToken(Long chatId, Integer messageId, String[] parts) {
        if (parts.length < 3) {
            if (messageId != null && parts.length == 2) {
                // Ya mando un token pero sin nombre de cuenta: borramos igual para no dejarlo expuesto.
                notifier.deleteMessage(chatId, messageId);
            }
            return "Uso: /token <token> <cuenta>\n" + TOKEN_HELP;
        }
        String token = parts[1];
        String accountName = parts[2];

        GameAccount account = gameAccountRepository.findByTelegramUserIdAndNameIgnoreCase(chatId, accountName)
                .orElseGet(GameAccount::new);
        boolean isNew = account.getId() == null;
        account.setTelegramUserId(chatId);
        account.setName(accountName);
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
        return (isNew ? "Cuenta '" + accountName + "' vinculada. " : "Token de '" + accountName + "' actualizado. ")
                + "Borre tu mensaje por seguridad. Tareas en error de esa cuenta se reactivaron.";
    }

    private String handleAutoUpgrade(Long chatId, String[] parts) {
        if (parts.length < 4) {
            return "Uso: /autosubir <habilidad> <recurso> <cuenta>\nHabilidades: " + SkillCatalog.availableSkillsText()
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
        String accountName = parts[3];

        Optional<GameAccount> accountOpt = gameAccountRepository.findByTelegramUserIdAndNameIgnoreCase(chatId, accountName);
        if (accountOpt.isEmpty()) {
            return "No tienes una cuenta llamada '" + accountName + "'. Primero manda /token <token> " + accountName + " para vincularla.";
        }
        GameAccount account = accountOpt.get();
        if (!account.isActive()) {
            return "El token de '" + accountName + "' esta marcado como invalido. Manda /token <nuevo_token> " + accountName + " primero.";
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
                + SkillCatalog.resourceDisplayName(costType) + " en la cuenta '" + accountName + "'.";
        if (account.isBusy(OffsetDateTime.now())) {
            reply += "\nOjo: ahora mismo esa cuenta ya tiene otra subida en curso (" + SkillCatalog.skillDisplayName(account.getBusySkillCode())
                    + "), asi que esta empezara automaticamente cuando esa termine, aprox a las "
                    + account.getBusyUntil().format(TIME_FORMAT) + ".";
        }
        return reply;
    }

    private String handleStop(Long chatId, String[] parts) {
        if (parts.length < 2) {
            return "Uso: /parar <cuenta>  o  /parar todas";
        }
        List<GameAccount> accounts = gameAccountRepository.findByTelegramUserId(chatId);
        if (accounts.isEmpty()) {
            return "No hay nada para detener.";
        }

        List<GameAccount> targets;
        if (parts[1].equalsIgnoreCase("todas")) {
            targets = accounts;
        } else {
            Optional<GameAccount> match = accounts.stream()
                    .filter(a -> parts[1].equalsIgnoreCase(a.getName()))
                    .findFirst();
            if (match.isEmpty()) {
                return "No tienes una cuenta llamada '" + parts[1] + "'.";
            }
            targets = List.of(match.get());
        }

        StringBuilder reply = new StringBuilder();
        boolean stoppedAny = false;
        for (GameAccount account : targets) {
            List<UpgradeTask> activeTasks = upgradeTaskRepository.findByGameAccountId(account.getId()).stream()
                    .filter(t -> t.getStatus() == UpgradeTaskStatus.ACTIVE)
                    .toList();
            if (activeTasks.isEmpty()) {
                continue;
            }
            activeTasks.forEach(t -> t.setStatus(UpgradeTaskStatus.PAUSED));
            upgradeTaskRepository.saveAll(activeTasks);
            stoppedAny = true;

            reply.append("Se detuvo la subida automatica de '").append(account.getName()).append("'.\n");
        }
        return stoppedAny ? reply.toString().strip() : "No hay nada para detener.";
    }

    private String handleForget(Long chatId, String[] parts) {
        if (parts.length < 2) {
            return "Uso: /borrar <cuenta>  o  /borrar todas";
        }
        List<GameAccount> accounts = gameAccountRepository.findByTelegramUserId(chatId);
        if (accounts.isEmpty()) {
            return "No tienes ninguna cuenta vinculada.";
        }

        List<GameAccount> targets;
        if (parts[1].equalsIgnoreCase("todas")) {
            targets = accounts;
        } else {
            Optional<GameAccount> match = accounts.stream()
                    .filter(a -> parts[1].equalsIgnoreCase(a.getName()))
                    .findFirst();
            if (match.isEmpty()) {
                return "No tienes una cuenta llamada '" + parts[1] + "'.";
            }
            targets = List.of(match.get());
        }

        StringBuilder reply = new StringBuilder();
        for (GameAccount account : targets) {
            List<UpgradeTask> tasks = upgradeTaskRepository.findByGameAccountId(account.getId());
            upgradeTaskRepository.deleteAll(tasks);
            gameAccountRepository.delete(account);
            reply.append("Se borro la cuenta '").append(account.getName()).append("' junto con su token y sus tareas.\n");
        }
        return reply.toString().strip();
    }

    private String handleStatus(Long chatId) {
        List<GameAccount> accounts = gameAccountRepository.findByTelegramUserId(chatId);
        if (accounts.isEmpty()) {
            return "No tienes ninguna cuenta vinculada. Manda /token <token> <cuenta> para empezar.";
        }

        StringBuilder sb = new StringBuilder();
        for (GameAccount account : accounts) {
            sb.append("Cuenta '").append(account.getName()).append("'");
            if (!account.isActive()) {
                sb.append(" [token invalido, manda /token de nuevo]");
            }
            sb.append(":\n");

            if (account.isBusy(OffsetDateTime.now())) {
                sb.append("  Ocupada: subiendo ").append(SkillCatalog.skillDisplayName(account.getBusySkillCode()))
                        .append(" hasta las ").append(account.getBusyUntil().format(TIME_FORMAT)).append("\n");
            }

            List<UpgradeTask> tasks = upgradeTaskRepository.findByGameAccountId(account.getId());
            if (tasks.isEmpty()) {
                sb.append("  Sin tareas configuradas.\n");
            } else {
                for (UpgradeTask task : tasks) {
                    sb.append("  - ").append(SkillCatalog.skillDisplayName(task.getSkillCode())).append(" (")
                            .append(SkillCatalog.resourceDisplayName(task.getCostType())).append("): ")
                            .append(task.getStatus());
                    if (task.getStatus() == UpgradeTaskStatus.ACTIVE && task.getNextRunAt() != null) {
                        sb.append(", proximo intento ").append(task.getNextRunAt().format(TIME_FORMAT));
                    }
                    if (task.getLastError() != null) {
                        sb.append(" (ultimo problema: ").append(task.getLastError()).append(")");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString().strip();
    }
}
