package com.easylink.ruleproduct.core.operator;

import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.OperatorType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ManualReviewOperator implements RuleOperator {

    @Override
    public OperatorType type() {
        return OperatorType.MANUAL_REVIEW;
    }

    @Override
    public DecisionResult evaluate(InvestigationStep step, List<Fact> facts) {
        return new DecisionResult(
                DecisionStatus.PENDING_MANUAL,
                step.stepName() + "：该步骤需要人工核查，系统仅提供已查询事实。",
                Map.of("judgmentBasis", step.decisionPolicy().judgmentBasis())
        );
    }
}
