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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flags")
public class FlagController {

    record CreateFlagRequest(String name, String description) {
    }

    record UpdateFlagRequest(Boolean enabled, Integer rolloutPercentage) {
    }

    record AddRuleRequest(String attribute, String operator, String value) {
    }

    private final FlagRepository flagRepository;
    private final TargetingRuleRepository ruleRepository;
    private final EvaluationService evaluationService;

    public FlagController(FlagRepository flagRepository, TargetingRuleRepository ruleRepository, EvaluationService evaluationService) {
        this.flagRepository = flagRepository;
        this.ruleRepository = ruleRepository;
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public FeatureFlag createFlag(@RequestBody CreateFlagRequest request) {
        return flagRepository.save(new FeatureFlag(request.name(), request.description()));
    }

    @GetMapping
    public List<FeatureFlag> listFlags() {
        return flagRepository.findAll();
    }

    @PutMapping("/{name}")
    public FeatureFlag updateFlag(@PathVariable String name, @RequestBody UpdateFlagRequest request) {
        FeatureFlag flag = flagRepository.findById(name).orElseThrow();
        if (request.enabled() != null) {
            flag.setEnabled(request.enabled());
        }
        if (request.rolloutPercentage() != null) {
            flag.setRolloutPercentage(request.rolloutPercentage());
        }
        return flagRepository.save(flag);
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
    public TargetingRule addRule(@PathVariable String name, @RequestBody AddRuleRequest request) {
        return ruleRepository.save(new TargetingRule(name, request.attribute(), request.operator(), request.value()));
    }

    @GetMapping("/{name}/rules")
    public List<TargetingRule> listRules(@PathVariable String name) {
        return ruleRepository.findByFlagName(name);
    }
}
