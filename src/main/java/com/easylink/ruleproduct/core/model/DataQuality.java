package com.easylink.ruleproduct.core.model;

public record DataQuality(
        String source,
        boolean complete,
        String missingReason
) {
    public static DataQuality complete(String source) {
        return new DataQuality(source, true, null);
    }

    public static DataQuality missing(String source, String reason) {
        return new DataQuality(source, false, reason);
    }
}
