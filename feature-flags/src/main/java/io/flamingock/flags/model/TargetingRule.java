package io.flamingock.flags.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "targeting_rules")
public class TargetingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flag_name")
    private String flagName;

    private String attribute;

    private String operator;

    private String value;

    @Column(name = "created_at")
    private Instant createdAt;

    protected TargetingRule() {
    }

    public TargetingRule(String flagName, String attribute, String operator, String value) {
        this.flagName = flagName;
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFlagName() {
        return flagName;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
