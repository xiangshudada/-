package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RatioCompareOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.RATIO_COMPARE;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        BigDecimal ratio = resolveRatio(step, facts);
        BigDecimal min = OperatorSupport.thresholdMin(step.thresholdProfile(), facts);
        BigDecimal max = OperatorSupport.thresholdMax(step.thresholdProfile(), facts);
        if (ratio == null || (min == null && max == null)) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少比例指标或阈值配置，无法判断。", Map.of());
        }
        boolean hit = (min != null && ratio.compareTo(min) >= 0)
                || (max != null && ratio.compareTo(max) <= 0);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ratio", ratio);
        details.put("min", min);
        details.put("max", max);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                "比例=" + ratio + "，阈值区间[min=" + min + ", max=" + max + "]，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private BigDecimal resolveRatio(InvestigationStep step, List<Fact> facts) {
        String metric = step.thresholdProfile().metric();
        if (metric != null) {
            var value = OperatorSupport.numberField(facts, metric);
            if (value.isPresent()) {
                return value.get();
            }
        }
        Map<String, Object> raw = step.thresholdProfile().raw();
        Object numeratorMetric = raw.get("numeratorMetric");
        Object denominatorMetric = raw.get("denominatorMetric");
        if (numeratorMetric instanceof String numerator && denominatorMetric instanceof String denominator) {
            BigDecimal numeratorValue = OperatorSupport.numberField(facts, numerator).orElse(null);
            BigDecimal denominatorValue = OperatorSupport.numberField(facts, denominator).orElse(null);
            return OperatorSupport.divide(numeratorValue, denominatorValue);
        }
        if (raw.get("countRatioMetric") instanceof String || raw.get("amountRatioMetric") instanceof String) {
            BigDecimal amountRatio = blockTradeAmountRatio(facts);
            if (amountRatio != null) {
                return amountRatio;
            }
            return blockTradeCountRatio(facts);
        }
        if ("shortTermNetInflowRatio".equals(metric)) {
            return directionAmountRatio(facts, "IN");
        }
        if ("postTradeTransferOutRatio".equals(metric)) {
            return directionAmountRatio(facts, "OUT");
        }
        if ("marketVolumeShare".equals(metric)) {
            return marketVolumeShare(facts);
        }
        return null;
    }

    private BigDecimal blockTradeAmountRatio(List<Fact> facts) {
        BigDecimal blockAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            BigDecimal amount = OperatorSupport.asNumber(sample.get("amount")).orElse(BigDecimal.ZERO);
            totalAmount = totalAmount.add(amount);
            Object tradeMode = sample.get("tradeMode");
            if (tradeMode instanceof String text && "BLOCK".equalsIgnoreCase(text.trim())) {
                blockAmount = blockAmount.add(amount);
            }
        }
        return OperatorSupport.divide(blockAmount, totalAmount);
    }

    private BigDecimal blockTradeCountRatio(List<Fact> facts) {
        int blockCount = 0;
        int totalCount = 0;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            totalCount++;
            Object tradeMode = sample.get("tradeMode");
            if (tradeMode instanceof String text && "BLOCK".equalsIgnoreCase(text.trim())) {
                blockCount++;
            }
        }
        return totalCount == 0 ? null : OperatorSupport.divide(BigDecimal.valueOf(blockCount), BigDecimal.valueOf(totalCount));
    }

    private BigDecimal directionAmountRatio(List<Fact> facts, String expectedDirection) {
        BigDecimal expectedAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            BigDecimal amount = OperatorSupport.asNumber(sample.get("amount")).orElse(BigDecimal.ZERO);
            totalAmount = totalAmount.add(amount.abs());
            Object direction = sample.get("direction");
            if (direction instanceof String text && expectedDirection.equals(text.trim().toUpperCase(Locale.ROOT))) {
                expectedAmount = expectedAmount.add(amount.abs());
            }
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0 && hasDomain(facts, "FundTransfer")) {
            return BigDecimal.ZERO;
        }
        return OperatorSupport.divide(expectedAmount, totalAmount);
    }

    private BigDecimal marketVolumeShare(List<Fact> facts) {
        BigDecimal tradeAmount = BigDecimal.ZERO;
        BigDecimal marketAmount = BigDecimal.ZERO;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            tradeAmount = tradeAmount.add(OperatorSupport.asNumber(sample.get("amount")).orElse(BigDecimal.ZERO));
            marketAmount = marketAmount.add(OperatorSupport.asNumber(sample.get("marketAmount"))
                    .or(() -> OperatorSupport.asNumber(sample.get("marketTotalAmount")))
                    .orElse(BigDecimal.ZERO));
        }
        return OperatorSupport.divide(tradeAmount, marketAmount);
    }

    private boolean hasDomain(List<Fact> facts, String domain) {
        return facts.stream().anyMatch(fact -> domain.equals(fact.domain()));
    }
}
