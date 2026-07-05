package com.easylink.ruleproduct.core.model;

import java.util.List;

public record StepExecutionResult(
        Long stepId,
        String stepCode,
        String stepName,
        OperatorType operatorType,
        List<Fact> facts,
        DecisionResult decision,
        ReportMapping reportMapping,
        List<VerificationExecutionResult> verificationResults
) {

    public StepExecutionResult(
            Long stepId,
            String stepCode,
            String stepName,
            OperatorType operatorType,
            List<Fact> facts,
            DecisionResult decision,
            ReportMapping reportMapping
    ) {
        this(stepId, stepCode, stepName, operatorType, facts, decision, reportMapping, List.of());
    }

    public StepExecutionResult {
        verificationResults = verificationResults == null ? List.of() : List.copyOf(verificationResults);
    }
}
