package com.easylink.ruleproduct.core.model;

import java.util.List;
import java.util.Set;

/**
 * 调查步骤下的具体核验项。
 *
 * <p>{@code InvestigationStep} 表达业务调查步骤；该模型表达可执行的自动核验或人工核查项。
 * 一个业务步骤可以包含多个核验项，每个核验项绑定自己的数据需求、算子和阈值配置。</p>
 *
 * @param itemId 核验项主键
 * @param itemCode 核验项编码
 * @param itemName 核验项名称
 * @param itemOrder 核验项在调查步骤内的顺序
 * @param methodText 核验项对应的具体方法文本
 * @param ruleText 核验项对应的规则文本
 * @param operatorType 核验项使用的算子类型
 * @param applicableClientTypes 适用客户类型
 * @param dataRequirements 核验项需要的标准领域数据
 * @param thresholdProfile 阈值或参数配置
 * @param decisionPolicy 核验项判断策略
 */
public record VerificationItem(
        Long itemId,
        String itemCode,
        String itemName,
        int itemOrder,
        String methodText,
        String ruleText,
        OperatorType operatorType,
        Set<ClientType> applicableClientTypes,
        List<DataRequirement> dataRequirements,
        ThresholdProfile thresholdProfile,
        DecisionPolicy decisionPolicy
) {

    public VerificationItem {
        applicableClientTypes = applicableClientTypes == null ? Set.of() : Set.copyOf(applicableClientTypes);
        dataRequirements = dataRequirements == null ? List.of() : List.copyOf(dataRequirements);
    }
}
