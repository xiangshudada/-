package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.util.List;
import java.util.Map;

public abstract class GenericNeutralOperator implements RuleOperator {

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        return new DecisionResult(
                DecisionStatus.NEUTRAL,
                step.operatorType() + " 已生成事实，具体判断可通过扩展算子实现。",
                Map.of("factCount", facts.size())
        );
    }

    public static GenericNeutralOperator of(OperatorType type) {
        return new GenericNeutralOperator() {
            @Override
            public OperatorType type() {
                return type;
            }
        };
    }
}
