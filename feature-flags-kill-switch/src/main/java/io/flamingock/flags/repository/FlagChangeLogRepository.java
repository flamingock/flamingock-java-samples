package io.flamingock.flags.repository;

import io.flamingock.flags.model.FlagChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlagChangeLogRepository extends JpaRepository<FlagChangeLog, UUID> {
    List<FlagChangeLog> findByFlagNameOrderByChangedAtDesc(String flagName);
}
