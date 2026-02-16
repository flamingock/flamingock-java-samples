package io.flamingock.flags.repository;

import io.flamingock.flags.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlagRepository extends JpaRepository<FeatureFlag, String> {
}
