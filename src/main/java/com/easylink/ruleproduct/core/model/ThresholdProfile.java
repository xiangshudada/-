package com.easylink.ruleproduct.core.model;

import java.math.BigDecimal;
import java.util.Map;

public record ThresholdProfile(
        String metric,
        BigDecimal min,
        BigDecimal max,
        Map<String, Object> raw
) {
    public static ThresholdProfile empty() {
        return new ThresholdProfile(null, null, null, Map.of());
    }
}
