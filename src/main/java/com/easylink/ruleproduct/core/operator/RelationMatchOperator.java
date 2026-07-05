package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RelationMatchOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.RELATION_MATCH;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        List<String> relationTypes = relationTypes(step);
        boolean hasEvidence = false;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            Object relationType = sample.get("relationType");
            if (relationType instanceof String text) {
                hasEvidence = true;
                if (relationTypes.isEmpty() || relationTypes.contains(text.trim().toUpperCase(Locale.ROOT))) {
                    return hit(text);
                }
            }
            for (String type : relationTypes) {
                Object flag = sample.get(toCamel(type));
                hasEvidence = hasEvidence || flag != null;
                if (OperatorSupport.booleanValue(flag)) {
                    return hit(type);
                }
            }
        }
        if (!hasEvidence) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少关系类型或关系标记数据，无法判断。", Map.of());
        }
        return new DecisionResult(DecisionStatus.NOT_HIT, "未发现配置范围内的关联关系。", Map.of("relationTypes", relationTypes));
    }

    @SuppressWarnings("unchecked")
    private List<String> relationTypes(InvestigationStep step) {
        Object value = step.thresholdProfile().raw().get("relationTypes");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(text -> text.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    private DecisionResult hit(String relationType) {
        return new DecisionResult(DecisionStatus.HIT, "发现关联关系：" + relationType + "。", Map.of("relationType", relationType));
    }

    private String toCamel(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char c : lower.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
