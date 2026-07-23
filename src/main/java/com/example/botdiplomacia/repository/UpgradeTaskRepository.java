package com.example.botdiplomacia.repository;

import com.example.botdiplomacia.model.UpgradeTask;
import com.example.botdiplomacia.model.UpgradeTaskStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UpgradeTaskRepository extends JpaRepository<UpgradeTask, Long> {
    List<UpgradeTask> findByStatusAndNextRunAtLessThanEqual(UpgradeTaskStatus status, OffsetDateTime now);

    List<UpgradeTask> findByGameAccountId(Long gameAccountId);

    Optional<UpgradeTask> findByGameAccountIdAndSkillCode(Long gameAccountId, String skillCode);
}
