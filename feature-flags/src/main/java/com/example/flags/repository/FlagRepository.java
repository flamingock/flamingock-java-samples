package com.example.flags.repository;

import com.example.flags.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlagRepository extends JpaRepository<FeatureFlag, String> {
}
