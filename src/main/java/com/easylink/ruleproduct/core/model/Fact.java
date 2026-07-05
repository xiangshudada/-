package com.easylink.ruleproduct.core.model;

import java.util.List;
import java.util.Map;

/**
 * 供规则算子判断使用的标准事实。
 *
 * <p>{@code FactFactory} 从 {@code DomainDataset} 中提取事实指标、证据样本和数据质量信息，
 * 形成该对象后交给 {@code RuleOperator}。它不保留客户物理字段语义，也不表示最终判断结果。</p>
 *
 * @param factCode 事实编码，通常由领域名称派生，用于区分同一步骤中的不同事实
 * @param domain 事实所属的标准领域名称
 * @param metrics 结构化事实指标，可包含统计值、画像值、关系标记等算子可读取的数据
 * @param samples 证据样本明细，用于报告引用、人工核查或需要明细判断的算子
 * @param dataQuality 数据质量状态，用于表达数据完整、缺失或不可用等情况
 */
public record Fact(
        String factCode,
        String domain,
        Map<String, Object> metrics,
        List<Map<String, Object>> samples,
        DataQuality dataQuality
) {
}
