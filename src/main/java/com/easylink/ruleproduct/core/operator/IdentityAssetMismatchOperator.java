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
public class IdentityAssetMismatchOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.IDENTITY_ASSET_MISMATCH;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        BigDecimal identityValue = identityValue(step, facts);
        BigDecimal assetValue = assetValue(facts);
        BigDecimal multiple = OperatorSupport.asNumber(step.thresholdProfile().raw().get("multiple")).orElse(BigDecimal.TEN);
        if (identityValue == null || assetValue == null || identityValue.compareTo(BigDecimal.ZERO) == 0) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少身份承载能力或账户资产/净转入数据，无法判断。", Map.of());
        }
        BigDecimal ratio = OperatorSupport.divide(assetValue, identityValue);
        boolean hit = ratio != null && ratio.compareTo(multiple) >= 0;
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("identityValue", identityValue);
        details.put("assetValue", assetValue);
        details.put("ratio", ratio);
        details.put("multiple", multiple);
        return new DecisionResult(
                hit ? DecisionStatus.HIT : DecisionStatus.NOT_HIT,
                "资金规模/身份承载能力=" + ratio + "，阈值倍数=" + multiple + "，判断结果=" + (hit ? "命中" : "未命中") + "。",
                details
        );
    }

    private BigDecimal identityValue(InvestigationStep step, List<Fact> facts) {
        Object configured = step.thresholdProfile().raw().get("identityMetric");
        if (configured instanceof String metric) {
            return OperatorSupport.numberField(facts, metric).orElse(null);
        }
        return OperatorSupport.numberField(facts, "annualIncome")
                .or(() -> OperatorSupport.numberField(facts, "registeredCapital"))
                .orElse(null);
    }

    private BigDecimal assetValue(List<Fact> facts) {
        for (String key : List.of("accountAsset", "totalAsset", "assetBalance", "netInflowAmount", "yearNetInflowAmount", "totalAmount")) {
            BigDecimal value = OperatorSupport.numberField(facts, key).orElse(null);
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return null;
    }
}
