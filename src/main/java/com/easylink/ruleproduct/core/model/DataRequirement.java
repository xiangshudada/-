package com.easylink.ruleproduct.core.model;

import java.util.List;
import java.util.Map;

/**
 * 调查步骤所需的数据声明。
 *
 * <p>该模型只描述规则步骤需要哪个标准领域数据、取哪些标准字段、附加哪些规则过滤条件，
 * 不承载客户物理表字段映射，也不承载疑点判断逻辑。</p>
 *
 * @param requirementId 数据需求主键
 * @param requirementCode 数据需求编码，用于在规则配置中稳定引用
 * @param domain 标准领域名称，用于选择对应的 {@code DomainDataAdapter} 和数据绑定
 * @param scope 数据需求作用范围，例如客户维度或交易对手维度
 * @param grain 数据粒度，例如明细、聚合或事件级数据
 * @param filters 规则层附加过滤条件，字段名应使用标准领域字段
 * @param fields 需要返回的标准领域字段；为空时由适配器按绑定默认字段返回
 * @param sampleLimit 样本返回上限，用于控制证据明细数量
 * @param missingDataPolicy 数据缺失时的处理策略
 */
public record DataRequirement(
        Long requirementId,
        String requirementCode,
        String domain,
        RequirementScope scope,
        DataGrain grain,
        Map<String, Object> filters,
        List<String> fields,
        int sampleLimit,
        MissingDataPolicy missingDataPolicy
) {
}
