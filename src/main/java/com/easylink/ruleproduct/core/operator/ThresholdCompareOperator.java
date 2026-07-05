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
public class ThresholdCompareOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.THRESHOLD_COMPARE;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        String metric = step.thresholdProfile().metric();
        BigDecimal threshold = step.thresholdProfile().min();
        if (metric == null || threshold == null) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少阈值配置，无法判断。", Map.of());
        }
        BigDecimal value = BigDecimal.ZERO;
        for (Fact fact : facts) {
            Object metricValue = fact.metrics().get(metric);
            if (metricValue instanceof Number number) {
                value = value.add(new BigDecimal(number.toString()));
            }
        }
        boolean hit = value.compareTo(threshold) >= 0;
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("metric", metric);
        details.put("value", value);
        details.put("threshold", threshold);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                metric + "=" + value + "，阈值=" + threshold + "，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }
}
