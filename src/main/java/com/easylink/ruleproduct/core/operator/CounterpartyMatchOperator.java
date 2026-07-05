package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CounterpartyMatchOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.COUNTERPARTY_MATCH;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        String metric = step.thresholdProfile().metric();
        if (metric != null) {
            BigDecimal value = OperatorSupport.numberField(facts, metric).orElse(null);
            if (value != null) {
                return value.compareTo(BigDecimal.ZERO) > 0 ? hit(Map.of("metric", metric, "value", value)) : notHit();
            }
        }
        boolean sawCounterparty = false;
        for (Map<String, Object> sample : OperatorSupport.samples(facts)) {
            if (OperatorSupport.booleanValue(sample.get("counterpartyIsClient"))) {
                return hit(Map.of("counterpartyIsClient", true));
            }
            Object clientId = sample.get("counterpartyClientId");
            if (clientId instanceof String text && !text.isBlank()) {
                return hit(Map.of("counterpartyClientId", text));
            }
            sawCounterparty = sawCounterparty || sample.containsKey("counterpartyIsClient") || sample.containsKey("counterpartyClientId");
        }
        if (!sawCounterparty) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "缺少交易对手客户标识数据，无法判断。", Map.of());
        }
        return notHit();
    }

    private DecisionResult hit(Map<String, Object> details) {
        return new DecisionResult(DecisionStatus.HIT, "交易对手匹配我司客户或存在可识别匹配特征。", details);
    }

    private DecisionResult notHit() {
        return new DecisionResult(DecisionStatus.NOT_HIT, "未发现交易对手匹配特征。", Map.of());
    }
}
