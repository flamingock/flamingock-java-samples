package io.flamingock.flags.service;

import io.flamingock.flags.model.FlagChangeLog;
import io.flamingock.flags.repository.FlagChangeLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlagChangeLogService {

    private final FlagChangeLogRepository repository;

    public FlagChangeLogService(FlagChangeLogRepository repository) {
        this.repository = repository;
    }

    public void record(String flagName, String changedBy, String action, String detail) {
        repository.save(new FlagChangeLog(flagName, changedBy, action, detail));
    }

    public List<FlagChangeLog> history(String flagName) {
        return repository.findByFlagNameOrderByChangedAtDesc(flagName);
    }
}
