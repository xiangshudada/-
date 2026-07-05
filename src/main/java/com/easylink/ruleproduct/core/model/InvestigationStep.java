package com.easylink.ruleproduct.core.model;

import java.util.List;
import java.util.Set;

public record InvestigationStep(
        Long stepId,
        String stepCode,
        String stepName,
        int stepOrder,
        String methodText,
        String ruleText,
        OperatorType operatorType,
        Set<ClientType> applicableClientTypes,
        List<DataRequirement> dataRequirements,
        ThresholdProfile thresholdProfile,
        DecisionPolicy decisionPolicy,
        ReportMapping reportMapping,
        List<VerificationItem> verificationItems
) {

    public InvestigationStep(
            Long stepId,
            String stepCode,
            String stepName,
            int stepOrder,
            String methodText,
            String ruleText,
            OperatorType operatorType,
            Set<ClientType> applicableClientTypes,
            List<DataRequirement> dataRequirements,
            ThresholdProfile thresholdProfile,
            DecisionPolicy decisionPolicy,
            ReportMapping reportMapping
    ) {
        this(
                stepId,
                stepCode,
                stepName,
                stepOrder,
                methodText,
                ruleText,
                operatorType,
                applicableClientTypes,
                dataRequirements,
                thresholdProfile,
                decisionPolicy,
                reportMapping,
                List.of(new VerificationItem(
                        stepId,
                        stepCode,
                        stepName,
                        1,
                        methodText,
                        ruleText,
                        operatorType,
                        applicableClientTypes,
                        dataRequirements,
                        thresholdProfile,
                        decisionPolicy
                ))
        );
    }

    public InvestigationStep {
        applicableClientTypes = applicableClientTypes == null ? Set.of() : Set.copyOf(applicableClientTypes);
        dataRequirements = dataRequirements == null ? List.of() : List.copyOf(dataRequirements);
        verificationItems = verificationItems == null ? List.of() : List.copyOf(verificationItems);
    }

    public List<VerificationItem> executableItems() {
        if (!verificationItems.isEmpty()) {
            return verificationItems;
        }
        return List.of(new VerificationItem(
                stepId,
                stepCode,
                stepName,
                1,
                methodText,
                ruleText,
                operatorType,
                applicableClientTypes,
                dataRequirements,
                thresholdProfile,
                decisionPolicy
        ));
    }

    public InvestigationStep viewFor(VerificationItem item) {
        return new InvestigationStep(
                stepId,
                stepCode,
                item.itemName(),
                stepOrder,
                item.methodText(),
                item.ruleText(),
                item.operatorType(),
                item.applicableClientTypes(),
                item.dataRequirements(),
                item.thresholdProfile(),
                item.decisionPolicy(),
                reportMapping,
                List.of(item)
        );
    }
}
