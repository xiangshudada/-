package com.easylink.ruleproduct.core.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 领域数据适配器返回的标准数据集。
 *
 * <p>{@code DomainDataAdapter} 负责把不同客户的数据源字段投影为标准领域字段，
 * 并通过该对象交给 {@code FactFactory} 生成事实指标和证据样本。</p>
 *
 * @param domain 标准领域名称，例如证券交易、客户画像、资金流水等
 * @param source 数据来源标识，可记录租户数据源、表名、接口名或适配器名称
 * @param rows 标准字段行数据；字段名应使用规则引擎内部约定的领域字段名
 */
public record DomainDataset(
        String domain,
        String source,
        List<Map<String, Object>> rows
) {
    /**
     * 返回数据行数，用于形成通用事实指标 {@code count}。
     */
    public int count() {
        return rows == null ? 0 : rows.size();
    }

    /**
     * 汇总指定数值字段，用于形成 {@code totalAmount}、{@code totalQuantity} 等事实指标。
     */
    public BigDecimal sum(String field) {
        if (rows == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object value = row.get(field);
            if (value instanceof Number number) {
                total = total.add(new BigDecimal(number.toString()));
            }
        }
        return total;
    }
}
