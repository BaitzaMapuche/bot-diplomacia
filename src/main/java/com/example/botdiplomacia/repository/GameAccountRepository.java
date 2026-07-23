package com.example.botdiplomacia.repository;

import com.example.botdiplomacia.model.GameAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameAccountRepository extends JpaRepository<GameAccount, Long> {
    List<GameAccount> findByTelegramUserId(Long telegramUserId);

    Optional<GameAccount> findByTelegramUserIdAndNameIgnoreCase(Long telegramUserId, String name);
}
