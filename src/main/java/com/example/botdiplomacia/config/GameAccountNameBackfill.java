package com.example.botdiplomacia.config;

import com.example.botdiplomacia.model.GameAccount;
import com.example.botdiplomacia.repository.GameAccountRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Le pone un nombre por defecto a las cuentas creadas antes de soportar
 * multiples cuentas por usuario (donde el nombre quedaria null tras el
 * ALTER TABLE). Idempotente: no hace nada si ya todas tienen nombre.
 */
@Component
public class GameAccountNameBackfill implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(GameAccountNameBackfill.class);
    private static final String DEFAULT_NAME = "principal";

    private final GameAccountRepository gameAccountRepository;

    public GameAccountNameBackfill(GameAccountRepository gameAccountRepository) {
        this.gameAccountRepository = gameAccountRepository;
    }

    @Override
    public void run(String... args) {
        List<GameAccount> withoutName = gameAccountRepository.findAll().stream()
                .filter(a -> a.getName() == null || a.getName().isBlank())
                .toList();
        if (withoutName.isEmpty()) {
            return;
        }
        for (GameAccount account : withoutName) {
            account.setName(DEFAULT_NAME);
        }
        gameAccountRepository.saveAll(withoutName);
        log.info("Se le puso el nombre '{}' a {} cuenta(s) sin nombre", DEFAULT_NAME, withoutName.size());
    }
}
