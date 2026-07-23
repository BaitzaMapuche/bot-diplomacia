package com.example.botdiplomacia.repository;

import com.example.botdiplomacia.model.TelegramChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramChatMessageRepository extends JpaRepository<TelegramChatMessage, Long> {
}
