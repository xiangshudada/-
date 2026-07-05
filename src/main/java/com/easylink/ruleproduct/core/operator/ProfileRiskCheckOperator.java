package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProfileRiskCheckOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.PROFILE_RISK_CHECK;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        Integer age = findIntegerMetric(facts, "age");
        if (age == null) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "未查询到客户年龄，无法执行画像风险检查。", Map.of());
        }
        BigDecimal min = step.thresholdProfile().min();
        BigDecimal max = step.thresholdProfile().max();
        boolean hit = (min != null && BigDecimal.valueOf(age).compareTo(min) <= 0)
                || (max != null && BigDecimal.valueOf(age).compareTo(max) >= 0);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("age", age);
        details.put("lowAgeThreshold", min);
        details.put("highAgeThreshold", max);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                "客户年龄=" + age + "，画像风险检查结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private Integer findIntegerMetric(List<Fact> facts, String metric) {
        for (Fact fact : facts) {
            Object value = fact.metrics().get(metric);
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return null;
    }
}
