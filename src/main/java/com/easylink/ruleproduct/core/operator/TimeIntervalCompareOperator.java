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
public class TimeIntervalCompareOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.TIME_INTERVAL_COMPARE;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        BigDecimal days = resolveDays(step, facts);
        BigDecimal min = OperatorSupport.thresholdMin(step.thresholdProfile(), facts);
        BigDecimal max = OperatorSupport.thresholdMax(step.thresholdProfile(), facts);
        if (days == null || (min == null && max == null)) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少时间间隔指标、日期字段或阈值配置，无法判断。", Map.of());
        }
        boolean hit = (min != null && days.compareTo(min) >= 0)
                || (max != null && days.compareTo(max) <= 0);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("days", days);
        details.put("min", min);
        details.put("max", max);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                "时间间隔=" + days + "天，阈值区间[min=" + min + ", max=" + max + "]，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private BigDecimal resolveDays(InvestigationStep step, List<Fact> facts) {
        String metric = step.thresholdProfile().metric();
        if (metric != null) {
            var value = OperatorSupport.numberField(facts, metric);
            if (value.isPresent()) {
                return value.get();
            }
        }
        String startField = rawString(step, "startDateField", "accountOpenDate");
        String endField = rawString(step, "endDateField", "tradeDate");
        BigDecimal minDays = null;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            var days = OperatorSupport.daysBetween(sample, startField, endField);
            if (days.isPresent() && (minDays == null || BigDecimal.valueOf(days.get()).compareTo(minDays) < 0)) {
                minDays = BigDecimal.valueOf(days.get());
            }
        }
        return minDays;
    }

    private String rawString(InvestigationStep step, String key, String defaultValue) {
        Object value = step.thresholdProfile().raw().get(key);
        return value instanceof String text && !text.isBlank() ? text : defaultValue;
    }
}
