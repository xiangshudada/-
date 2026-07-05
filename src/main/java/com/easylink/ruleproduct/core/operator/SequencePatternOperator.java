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
public class SequencePatternOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.SEQUENCE_PATTERN;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        String metric = step.thresholdProfile().metric();
        BigDecimal value = metric == null ? null : OperatorSupport.numberField(facts, metric).orElse(null);
        BigDecimal min = OperatorSupport.thresholdMin(step.thresholdProfile(), facts);
        BigDecimal max = OperatorSupport.thresholdMax(step.thresholdProfile(), facts);
        if (value == null && step.thresholdProfile().raw().get("sequence") instanceof List<?>) {
            value = sequencePresent(facts) ? BigDecimal.ONE : BigDecimal.ZERO;
            min = min == null ? BigDecimal.ONE : min;
        }
        if (value == null || (min == null && max == null)) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少时序指标或阈值配置，无法判断。", Map.of());
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
                "时序指标=" + value + "，阈值区间[min=" + min + ", max=" + max + "]，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private boolean sequencePresent(List<Fact> facts) {
        boolean hasTrade = false;
        boolean hasTransferOut = false;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            Object tradeMode = sample.get("tradeMode");
            Object direction = sample.get("direction");
            hasTrade = hasTrade || tradeMode instanceof String;
            hasTransferOut = hasTransferOut || (direction instanceof String text && "OUT".equalsIgnoreCase(text.trim()));
        }
        return hasTrade && hasTransferOut;
    }
}
