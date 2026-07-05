package com.easylink.ruleproduct.core.model;

import java.util.Map;

public record DecisionResult(
        DecisionStatus status,
        String summary,
        Map<String, Object> details
) {
}
