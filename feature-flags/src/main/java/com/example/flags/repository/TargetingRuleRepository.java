package com.example.flags.repository;

import com.example.flags.model.TargetingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TargetingRuleRepository extends JpaRepository<TargetingRule, UUID> {
    List<TargetingRule> findByFlagName(String flagName);
}
