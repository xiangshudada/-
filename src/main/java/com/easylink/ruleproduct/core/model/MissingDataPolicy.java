package com.easylink.ruleproduct.core.model;

/**
 * 数据缺失时的处理策略。
 */
public enum MissingDataPolicy {
    /**
     * 返回空事实并带上缺失原因，由算子或报告层决定是否继续。
     */
    RETURN_EMPTY_WITH_REASON;

    public static MissingDataPolicy fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RETURN_EMPTY_WITH_REASON;
        }
        return MissingDataPolicy.valueOf(code.trim());
    }
}
