package com.easylink.ruleproduct.core.model;

/**
 * 数据需求的查询范围。
 *
 * 该枚举描述规则希望在什么时间或业务范围内取数，属于引擎协议值，
 * 不应在代码中散落字符串。
 */
public enum RequirementScope {
    /**
     * 预警明细或预警当天上下文。
     */
    ALERT_DETAIL,

    /**
     * 预警窗口内的数据，默认窗口由执行请求或 QueryContext 决定。
     */
    WARNING_PERIOD;

    public static RequirementScope fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("数据需求范围不能为空");
        }
        return RequirementScope.valueOf(code.trim());
    }
}
