package io.flamingock.flags.controller;

import io.flamingock.flags.model.FeatureFlag;
import io.flamingock.flags.model.FlagChangeLog;
import io.flamingock.flags.model.TargetingRule;
import io.flamingock.flags.repository.FlagRepository;
import io.flamingock.flags.repository.TargetingRuleRepository;
import io.flamingock.flags.service.EvaluationService;
import io.flamingock.flags.service.FlagChangeLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flags")
public class FlagController {

    record CreateFlagRequest(String name, String description) {
    }

    record UpdateFlagRequest(Boolean enabled, Integer rolloutPercentage, Boolean forceDisabled, String changedBy) {
    }

    record AddRuleRequest(String attribute, String operator, String value) {
    }

    private final FlagRepository repository;
    private final TargetingRuleRepository ruleRepository;
    private final EvaluationService evaluationService;
    private final FlagChangeLogService changeLogService;

    public FlagController(FlagRepository repository,
                          TargetingRuleRepository ruleRepository,
                          EvaluationService evaluationService,
                          FlagChangeLogService changeLogService) {
        this.repository = repository;
        this.ruleRepository = ruleRepository;
        this.evaluationService = evaluationService;
        this.changeLogService = changeLogService;
    }

    @PostMapping
    public FeatureFlag create(@RequestBody CreateFlagRequest req) {
        return repository.save(new FeatureFlag(req.name(), req.description()));
    }

    @GetMapping
    public List<FeatureFlag> list() {
        return repository.findAll();
    }

    @PutMapping("/{name}")
    public FeatureFlag update(
            @PathVariable String name,
            @RequestBody UpdateFlagRequest req) {
        FeatureFlag flag = repository.findById(name).orElseThrow();
        String by = req.changedBy() != null ? req.changedBy() : "system";

        if (req.enabled() != null && req.enabled() != flag.isEnabled()) {
            String detail = flag.isEnabled() + " → " + req.enabled();
            flag.setEnabled(req.enabled());
            changeLogService.record(name, by, req.enabled() ? "ENABLED" : "DISABLED", detail);
        }
        if (req.rolloutPercentage() != null && !req.rolloutPercentage().equals(flag.getRolloutPercentage())) {
            String detail = flag.getRolloutPercentage() + "% → " + req.rolloutPercentage() + "%";
            flag.setRolloutPercentage(req.rolloutPercentage());
            changeLogService.record(name, by, "ROLLOUT_UPDATED", detail);
        }
        if (req.forceDisabled() != null && req.forceDisabled() != flag.isForceDisabled()) {
            flag.setForceDisabled(req.forceDisabled());
            changeLogService.record(name, by,
                    req.forceDisabled() ? "KILL_SWITCH_ON" : "KILL_SWITCH_OFF",
                    "kill switch " + (req.forceDisabled() ? "activated" : "deactivated"));
        }

        return repository.save(flag);
    }

    @GetMapping("/evaluate/{name}")
    public EvaluationService.EvalResult evaluate(
            @PathVariable String name,
            @RequestParam String userId,
            @RequestParam Map<String, String> allParams) {
        Map<String, String> attrs = new HashMap<>(allParams);
        attrs.remove("userId");
        return evaluationService.evaluate(name, userId, attrs);
    }

    @PostMapping("/{name}/rules")
    public TargetingRule addRule(@PathVariable String name, @RequestBody AddRuleRequest req) {
        return ruleRepository.save(new TargetingRule(name, req.attribute(), req.operator(), req.value()));
    }

    @GetMapping("/{name}/rules")
    public List<TargetingRule> listRules(@PathVariable String name) {
        return ruleRepository.findByFlagName(name);
    }

    @GetMapping("/{name}/log")
    public List<FlagChangeLog> changeLog(@PathVariable String name) {
        return changeLogService.history(name);
    }
}
