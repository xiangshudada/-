package com.easylink.ruleproduct.core.adapter;

import java.time.LocalDate;

/**
 * 单次规则执行的数据查询上下文。
 *
 * <p>该对象把租户、数据源配置、指标、客户和预警窗口传递给 {@code DomainDataAdapter}，
 * 供适配器选择正确的数据绑定、客户数据源和查询时间范围。</p>
 *
 * @param tenantId 租户标识，用于区分不同客户机构
 * @param dataSourceProfile 数据源配置标识，用于区分同一租户下的不同接入环境或数据版本
 * @param indicatorCode 当前执行的指标编码，仅作为上下文传递，不应在通用适配器中写指标特例
 * @param clientId 被核查客户标识
 * @param alertDate 预警日期
 * @param warningStartDate 预警窗口开始日期
 * @param warningEndDate 预警窗口结束日期
 */
public record QueryContext(
        String tenantId,
        String dataSourceProfile,
        String indicatorCode,
        String clientId,
        LocalDate alertDate,
        LocalDate warningStartDate,
        LocalDate warningEndDate
) {
}
