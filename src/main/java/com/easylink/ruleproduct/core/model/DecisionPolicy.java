package com.easylink.ruleproduct.core.model;

public record DecisionPolicy(
        String judgmentBasis,
        boolean manualReviewRequired
) {
}
