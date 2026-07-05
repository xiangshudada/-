package com.easylink.ruleproduct.core.model;

import java.util.List;

public record IndicatorModel(
        String indicatorCode,
        String indicatorName,
        String scenario,
        String ruleVersion,
        List<RiskSection> sections
) {
}
