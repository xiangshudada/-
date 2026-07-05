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
        BigDecimal min = OperatorSupport.thresholdMin(step.thresholdProfile(), facts);
        BigDecimal max = OperatorSupport.thresholdMax(step.thresholdProfile(), facts);
        if (metric == null || (min == null && max == null)) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少阈值配置，无法判断。", Map.of());
        }
        BigDecimal value = metricValue(metric, facts);
        if (value == null) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少指标 " + metric + " 的事实值，无法判断。", Map.of());
        }
        boolean hit = (min != null && value.compareTo(min) >= 0)
                || (max != null && value.compareTo(max) <= 0);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("metric", metric);
        details.put("value", value);
        details.put("min", min);
        details.put("max", max);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                metric + "=" + value + "，阈值区间[min=" + min + ", max=" + max + "]，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private BigDecimal metricValue(String metric, List<Fact> facts) {
        BigDecimal value = BigDecimal.ZERO;
        boolean found = false;
        for (Fact fact : facts) {
            Object metricValue = fact.metrics().get(metric);
            if (metricValue instanceof Number number) {
                value = value.add(new BigDecimal(number.toString()));
                found = true;
            }
        }
        if (found) {
            return value;
        }
        if ("shortTermNetInflowAmount".equals(metric)) {
            return shortTermNetInflowAmount(facts);
        }
        return OperatorSupport.numberField(facts, metric).orElse(null);
    }

    private BigDecimal shortTermNetInflowAmount(List<Fact> facts) {
        BigDecimal netAmount = BigDecimal.ZERO;
        boolean foundTransfer = false;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            BigDecimal amount = OperatorSupport.asNumber(sample.get("amount")).orElse(null);
            Object direction = sample.get("direction");
            if (amount == null || !(direction instanceof String text)) {
                continue;
            }
            if ("IN".equalsIgnoreCase(text.trim())) {
                netAmount = netAmount.add(amount.abs());
                foundTransfer = true;
            } else if ("OUT".equalsIgnoreCase(text.trim())) {
                netAmount = netAmount.subtract(amount.abs());
                foundTransfer = true;
            }
        }
        return foundTransfer ? netAmount : null;
    }
}
