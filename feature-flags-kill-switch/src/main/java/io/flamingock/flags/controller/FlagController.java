package io.flamingock.flags.controller;

import io.flamingock.flags.model.FeatureFlag;
import io.flamingock.flags.model.TargetingRule;
import io.flamingock.flags.repository.FlagRepository;
import io.flamingock.flags.repository.TargetingRuleRepository;
import io.flamingock.flags.service.EvaluationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flags")
public class FlagController {

    record CreateFlagRequest(String name, String description) {
    }

    record UpdateFlagRequest(Boolean enabled, Integer rolloutPercentage, Boolean forceDisabled,
                             Instant activateAt, Instant deactivateAt) {
    }

    record AddRuleRequest(String attribute, String operator, String value) {
    }

    private final FlagRepository repository;
    private final TargetingRuleRepository ruleRepository;
    private final EvaluationService evaluationService;

    public FlagController(FlagRepository repository,
                          TargetingRuleRepository ruleRepository,
                          EvaluationService evaluationService) {
        this.repository = repository;
        this.ruleRepository = ruleRepository;
        this.evaluationService = evaluationService;
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
        if (req.enabled() != null) flag.setEnabled(req.enabled());
        if (req.rolloutPercentage() != null) flag.setRolloutPercentage(req.rolloutPercentage());
        if (req.forceDisabled() != null) flag.setForceDisabled(req.forceDisabled());
        if (req.activateAt() != null) flag.setActivateAt(req.activateAt());
        if (req.deactivateAt() != null) flag.setDeactivateAt(req.deactivateAt());
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
}
