package com.easylink.ruleproduct.core.model;

import java.util.List;

public record RiskSection(
        Long sectionId,
        String riskType,
        String crimeType,
        String riskPoint,
        int sectionOrder,
        List<InvestigationStep> steps
) {
}
