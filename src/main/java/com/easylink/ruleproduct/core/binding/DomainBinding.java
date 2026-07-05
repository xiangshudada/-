package com.easylink.ruleproduct.core.binding;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 描述某个租户的物理数据源如何投影到标准 AML 领域数据。
 *
 * fieldMappings 的 key 是标准字段名，value 是客户物理列名。
 * codeMappings 用于读取行数据后做码值归一化，使引擎后续只处理标准业务值。
 */
public record DomainBinding(
        Long bindingId,
        String tenantId,
        String dataSourceProfile,
        String domain,
        String sourceName,
        String tableName,
        String clientIdColumn,
        String dateColumn,
        Map<String, String> fieldMappings,
        Map<String, Object> staticFilters,
        Map<String, Map<String, String>> codeMappings
) {
    public DomainBinding {
        fieldMappings = fieldMappings == null ? Map.of() : Map.copyOf(fieldMappings);
        staticFilters = staticFilters == null ? Map.of() : Map.copyOf(staticFilters);
        codeMappings = normalizeCodeMappings(codeMappings);
    }

    public String sourceColumn(String canonicalField) {
        return fieldMappings.get(canonicalField);
    }

    /**
     * 存在码值映射时返回标准值；未配置的原始码值会保留，避免证据被静默丢弃。
     */
    public Object canonicalValue(String canonicalField, Object sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        Map<String, String> mapping = codeMappings.get(canonicalField);
        if (mapping == null || mapping.isEmpty()) {
            return sourceValue;
        }
        String key = String.valueOf(sourceValue);
        return mapping.containsKey(key) ? mapping.get(key) : sourceValue;
    }

    private static Map<String, Map<String, String>> normalizeCodeMappings(Map<String, Map<String, String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, String>> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() == null ? Map.of() : Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(normalized);
    }
}
