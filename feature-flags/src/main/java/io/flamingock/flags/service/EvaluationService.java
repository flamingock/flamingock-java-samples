package io.flamingock.flags.service;

import io.flamingock.flags.model.FeatureFlag;
import io.flamingock.flags.model.TargetingRule;
import io.flamingock.flags.repository.FlagRepository;
import io.flamingock.flags.repository.TargetingRuleRepository;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    public record EvalResult(boolean enabled, String reason) {
    }

    private final FlagRepository flagRepository;
    private final TargetingRuleRepository ruleRepository;

    public EvaluationService(FlagRepository flagRepository, TargetingRuleRepository ruleRepository) {
        this.flagRepository = flagRepository;
        this.ruleRepository = ruleRepository;
    }

    public EvalResult evaluate(String flagName, String userId, Map<String, String> attrs) {
        FeatureFlag flag = flagRepository.findById(flagName).orElse(null);
        if (flag == null) {
            return new EvalResult(false, "flag not found");
        }

        if (!flag.isEnabled()) {
            return new EvalResult(false, "flag disabled");
        }

        List<TargetingRule> rules = ruleRepository.findByFlagName(flagName);
        if (!rules.isEmpty()) {
            for (TargetingRule rule : rules) {
                if (matches(rule, attrs)) {
                    return new EvalResult(true, "targeting rule matched: " + rule.getAttribute() + " " + rule.getOperator() + " " + rule.getValue());
                }
            }
        }

        if (flag.getRolloutPercentage() >= 100) {
            return new EvalResult(true, "rollout 100%");
        }

        int bucket = bucket(flagName, userId);
        boolean inRollout = bucket < flag.getRolloutPercentage();
        return new EvalResult(inRollout, inRollout
                ? "in rollout bucket " + bucket + " < " + flag.getRolloutPercentage() + "%"
                : "outside rollout bucket " + bucket + " >= " + flag.getRolloutPercentage() + "%");
    }

    private boolean matches(TargetingRule rule, Map<String, String> attrs) {
        String attrValue = attrs.get(rule.getAttribute());
        if (attrValue == null) {
            return false;
        }
        return switch (rule.getOperator()) {
            case "equals" -> attrValue.equals(rule.getValue());
            case "contains" -> attrValue.contains(rule.getValue());
            case "in" -> List.of(rule.getValue().split(",")).contains(attrValue);
            case "starts_with" -> attrValue.startsWith(rule.getValue());
            default -> false;
        };
    }

    private int bucket(String flagName, String userId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((flagName + ":" + userId).getBytes());
            return Math.floorMod(ByteBuffer.wrap(hash).getInt(), 100);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
