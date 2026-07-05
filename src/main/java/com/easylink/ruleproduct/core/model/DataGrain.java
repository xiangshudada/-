package com.easylink.ruleproduct.core.model;

/**
 * 数据需求返回的数据粒度。
 */
public enum DataGrain {
    /**
     * 明细粒度，通常保留交易、转账、事件等逐笔样本。
     */
    DETAIL,

    /**
     * 账户汇总粒度，通常用于客户画像、资产概览等单客户摘要。
     */
    ACCOUNT_SUMMARY;

    public static DataGrain fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("数据粒度不能为空");
        }
        return DataGrain.valueOf(code.trim());
    }
}
