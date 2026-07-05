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
public class PriceDeviationOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.PRICE_DEVIATION;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        String metric = step.thresholdProfile().metric();
        BigDecimal value = metric == null ? null : OperatorSupport.numberField(facts, metric).orElse(null);
        if (value == null) {
            value = calculateFromSamples(step, facts);
        }
        BigDecimal min = OperatorSupport.thresholdMin(step.thresholdProfile(), facts);
        BigDecimal max = OperatorSupport.thresholdMax(step.thresholdProfile(), facts);
        if (value == null || (min == null && max == null)) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少价格偏离指标、基准价格或阈值配置，无法判断。", Map.of());
        }
        boolean hit = (min != null && value.abs().compareTo(min) >= 0)
                || (max != null && value.abs().compareTo(max) <= 0);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("metric", metric);
        details.put("value", value);
        details.put("min", min);
        details.put("max", max);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                "价格偏离指标=" + value + "，阈值区间[min=" + min + ", max=" + max + "]，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private BigDecimal calculateFromSamples(InvestigationStep step, List<Fact> facts) {
        if ("marketValueBenefitAmount".equals(step.thresholdProfile().metric())) {
            return marketValueBenefit(facts);
        }
        BigDecimal maxDeviation = null;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            BigDecimal price = OperatorSupport.asNumber(sample.get("price")).orElse(null);
            BigDecimal benchmark = firstBenchmark(sample);
            if (price == null || benchmark == null) {
                continue;
            }
            BigDecimal deviation = OperatorSupport.divide(price.subtract(benchmark), benchmark);
            if (deviation != null && (maxDeviation == null || deviation.abs().compareTo(maxDeviation.abs()) > 0)) {
                maxDeviation = deviation;
            }
        }
        return maxDeviation;
    }

    private BigDecimal marketValueBenefit(List<Fact> facts) {
        BigDecimal total = BigDecimal.ZERO;
        boolean calculated = false;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            BigDecimal price = OperatorSupport.asNumber(sample.get("price")).orElse(null);
            BigDecimal benchmark = firstBenchmark(sample);
            BigDecimal quantity = OperatorSupport.asNumber(sample.get("quantity")).orElse(null);
            if (price == null || benchmark == null || quantity == null) {
                continue;
            }
            total = total.add(benchmark.subtract(price).abs().multiply(quantity));
            calculated = true;
        }
        return calculated ? total : null;
    }

    private BigDecimal firstBenchmark(Map<String, Object> sample) {
        for (String field : List.of("benchmarkPrice", "previousClosePrice", "marketAveragePrice", "valuationPrice")) {
            BigDecimal value = OperatorSupport.asNumber(sample.get(field)).orElse(null);
            if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
                return value;
            }
        }
        return null;
    }
}
