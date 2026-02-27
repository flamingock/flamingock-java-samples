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
@Table(name = "flag_change_log")
public class FlagChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flag_name")
    private String flagName;

    @Column(name = "changed_by")
    private String changedBy;

    private String action;  // ENABLED, DISABLED, KILL_SWITCH_ON, KILL_SWITCH_OFF, ROLLOUT_UPDATED

    private String detail;  // "40% → 60%", "kill switch activated"

    @Column(name = "changed_at")
    private Instant changedAt;

    public FlagChangeLog() {
    }

    public FlagChangeLog(String flagName, String changedBy, String action, String detail) {
        this.flagName = flagName;
        this.changedBy = changedBy;
        this.action = action;
        this.detail = detail;
        this.changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getFlagName() {
        return flagName;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
