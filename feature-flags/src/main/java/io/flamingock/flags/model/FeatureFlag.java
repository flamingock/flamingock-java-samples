package io.flamingock.flags.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    private String name;

    private String description;

    private boolean enabled;

    @Column(name = "rollout_percentage")
    private int rolloutPercentage = 100;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected FeatureFlag() {
    }

    public FeatureFlag(String name, String description) {
        this.name = name;
        this.description = description;
        this.enabled = false;
        this.rolloutPercentage = 100;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public int getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(int rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
