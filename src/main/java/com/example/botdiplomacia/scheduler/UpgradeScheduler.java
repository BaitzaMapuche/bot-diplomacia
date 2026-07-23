package com.example.botdiplomacia.scheduler;

import com.example.botdiplomacia.diplomacia.DiplomaciaApiClient;
import com.example.botdiplomacia.diplomacia.SkillCatalog;
import com.example.botdiplomacia.diplomacia.UpgradeResult;
import com.example.botdiplomacia.model.GameAccount;
import com.example.botdiplomacia.model.UpgradeTask;
import com.example.botdiplomacia.model.UpgradeTaskStatus;
import com.example.botdiplomacia.repository.GameAccountRepository;
import com.example.botdiplomacia.repository.UpgradeTaskRepository;
import com.example.botdiplomacia.service.TelegramNotifier;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * El juego solo permite una subida "pendiente" a la vez por cuenta (no por
 * habilidad). Por eso el gate real de concurrencia es GameAccount.busyUntil,
 * no el nextRunAt de cada tarea individual: aunque varias tareas esten ACTIVE
 * y "listas" a la vez, solo se intenta una por cuenta en cada vuelta, y solo
 * si la cuenta no sigue ocupada por una subida anterior todavia en curso.
 */
@Component
public class UpgradeScheduler {
    private static final Logger log = LoggerFactory.getLogger(UpgradeScheduler.class);
    private static final long SAFETY_MARGIN_MS = 2000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final UpgradeTaskRepository upgradeTaskRepository;
    private final GameAccountRepository gameAccountRepository;
    private final DiplomaciaApiClient diplomaciaApiClient;
    private final TelegramNotifier notifier;

    public UpgradeScheduler(UpgradeTaskRepository upgradeTaskRepository,
                             GameAccountRepository gameAccountRepository,
                             DiplomaciaApiClient diplomaciaApiClient,
                             TelegramNotifier notifier) {
        this.upgradeTaskRepository = upgradeTaskRepository;
        this.gameAccountRepository = gameAccountRepository;
        this.diplomaciaApiClient = diplomaciaApiClient;
        this.notifier = notifier;
    }

    @Scheduled(fixedDelayString = "${diplomacia.scheduler-interval-ms:5000}")
    public void processDueTasks() {
        OffsetDateTime now = OffsetDateTime.now();
        List<UpgradeTask> dueTasks = upgradeTaskRepository.findByStatusAndNextRunAtLessThanEqual(UpgradeTaskStatus.ACTIVE, now);

        Map<Long, List<UpgradeTask>> byAccount = dueTasks.stream()
                .collect(Collectors.groupingBy(UpgradeTask::getGameAccountId));

        for (Map.Entry<Long, List<UpgradeTask>> entry : byAccount.entrySet()) {
            try {
                processAccount(entry.getKey(), entry.getValue(), now);
            } catch (Exception e) {
                log.error("Error procesando cuenta {}", entry.getKey(), e);
            }
        }
    }

    private void processAccount(Long accountId, List<UpgradeTask> dueTasksForAccount, OffsetDateTime now) {
        Optional<GameAccount> accountOpt = gameAccountRepository.findById(accountId);
        if (accountOpt.isEmpty() || !accountOpt.get().isActive()) {
            return;
        }
        GameAccount account = accountOpt.get();

        // Ya hay una subida en curso en el juego para esta cuenta: no se puede mandar otra todavia.
        if (account.isBusy(now)) {
            return;
        }

        // Solo se intenta UNA tarea por cuenta en esta vuelta: la que lleva mas tiempo esperando.
        UpgradeTask task = dueTasksForAccount.stream()
                .min(Comparator.comparing(UpgradeTask::getNextRunAt))
                .orElse(null);
        if (task == null) {
            return;
        }

        UpgradeResult result = diplomaciaApiClient.upgradeSkill(account.getSessionToken(), task.getSkillCode(), task.getCostType());

        if (result.isSuccess()) {
            // Margen explicito sobre el cooldown que devuelve la API, para no pegarle justo al limite.
            long waitMs = Math.max(result.getCooldownMs(), 0) + SAFETY_MARGIN_MS;
            OffsetDateTime nextAvailable = OffsetDateTime.now().plus(Duration.ofMillis(waitMs));
            task.setNextRunAt(nextAvailable);
            task.setLastError(null);
            upgradeTaskRepository.save(task);

            account.setBusyUntil(nextAvailable);
            account.setBusySkillCode(task.getSkillCode());
            gameAccountRepository.save(account);

            if (account.isNotifyOnStart()) {
                notifier.sendMessage(account.getTelegramUserId(), buildSuccessMessage(task, result, nextAvailable));
            }
        } else if (result.isAuthExpired()) {
            account.setActive(false);
            account.setBusyUntil(null);
            account.setBusySkillCode(null);
            gameAccountRepository.save(account);

            List<UpgradeTask> allTasks = upgradeTaskRepository.findByGameAccountId(account.getId());
            for (UpgradeTask t : allTasks) {
                if (t.getStatus() == UpgradeTaskStatus.ACTIVE) {
                    t.setStatus(UpgradeTaskStatus.ERROR);
                    t.setLastError("Token expirado o invalido");
                }
            }
            upgradeTaskRepository.saveAll(allTasks);

            notifier.sendMessage(account.getTelegramUserId(),
                    "Tu sesion de diplomacia.com.tr expiro o ya no es valida. Todas tus subidas automaticas se pausaron. Manda /token <nuevo_token> para reactivarlas.");
        } else {
            task.setStatus(UpgradeTaskStatus.ERROR);
            task.setLastError(result.getErrorMessage());
            upgradeTaskRepository.save(task);
            notifier.sendMessage(account.getTelegramUserId(),
                    "No se pudo subir " + SkillCatalog.skillDisplayName(task.getSkillCode()) + ": " + result.getErrorMessage()
                            + ". La tarea quedo pausada, usa /autosubir " + SkillCatalog.skillDisplayName(task.getSkillCode())
                            + " " + SkillCatalog.resourceDisplayName(task.getCostType()) + " para reintentar.");
        }
    }

    private String buildSuccessMessage(UpgradeTask task, UpgradeResult result, OffsetDateTime nextAvailable) {
        StringBuilder sb = new StringBuilder();
        sb.append(SkillCatalog.skillDisplayName(task.getSkillCode())).append(": subida en marcha");
        if (result.getCurrentLevel() != null && result.getTargetLevel() != null) {
            sb.append(" (nivel ").append(result.getCurrentLevel()).append(" -> ").append(result.getTargetLevel()).append(")");
        }
        if (result.getCost() != null) {
            sb.append(", costo ").append(result.getCost()).append(" ").append(SkillCatalog.resourceDisplayName(task.getCostType()));
        }
        sb.append(". Estara lista aprox a las ").append(nextAvailable.format(TIME_FORMAT)).append(".");
        return sb.toString();
    }
}
