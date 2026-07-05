package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ObjectiveDescribeOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.OBJECTIVE_DESCRIBE;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        int totalRows = facts.stream()
                .mapToInt(fact -> ((Number) fact.metrics().getOrDefault("count", 0)).intValue())
                .sum();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("factCount", facts.size());
        details.put("sampleCount", totalRows);
        return new DecisionResult(
                DecisionStatus.NEUTRAL,
                step.stepName() + "：已按数据需求形成客观事实，共 " + facts.size() + " 类事实，样本 " + totalRows + " 条。",
                details
        );
    }
}
