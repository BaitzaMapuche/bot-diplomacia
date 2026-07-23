package com.example.botdiplomacia.repository;

import com.example.botdiplomacia.model.AuthorizedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorizedUserRepository extends JpaRepository<AuthorizedUser, Long> {
}
