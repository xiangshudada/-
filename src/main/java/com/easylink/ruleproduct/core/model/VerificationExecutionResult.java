package com.easylink.ruleproduct.core.model;

import java.util.List;

/**
 * 单个核验项的执行结果。
 *
 * <p>该结果保留核验项使用的算子、事实和判断结果，供调查步骤聚合、报告引用和问题定位使用。</p>
 *
 * @param itemId 核验项主键
 * @param itemCode 核验项编码
 * @param itemName 核验项名称
 * @param operatorType 实际执行的算子类型
 * @param facts 核验项消费的事实数据
 * @param decision 核验项判断结果
 */
public record VerificationExecutionResult(
        Long itemId,
        String itemCode,
        String itemName,
        OperatorType operatorType,
        List<Fact> facts,
        DecisionResult decision
) {
}
