package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.adapter.DomainDataAdapter;
import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.binding.DomainBinding;
import com.easylink.ruleproduct.core.binding.DomainBindingRepository;
import com.easylink.ruleproduct.core.model.DataRequirement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于租户领域绑定的通用 JDBC 适配器。
 * 客户只需要把自己的表、字段和码值映射到标准 AML 领域，规则引擎本身保持稳定。
 */
@Component
public class ConfigurableJdbcDomainDataAdapter implements DomainDataAdapter {

    private final JdbcTemplate jdbcTemplate;
    private final DomainBindingRepository bindingRepository;

    public ConfigurableJdbcDomainDataAdapter(JdbcTemplate jdbcTemplate, DomainBindingRepository bindingRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.bindingRepository = bindingRepository;
    }

    @Override
    public boolean supports(String domain) {
        return false;
    }

    @Override
    public boolean supports(String domain, QueryContext context) {
        return context != null && bindingRepository
                .findBinding(context.tenantId(), context.dataSourceProfile(), domain)
                .isPresent();
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public DomainDataset query(DataRequirement requirement, QueryContext context) {
        DomainBinding binding = bindingRepository
                .findBinding(context.tenantId(), context.dataSourceProfile(), requirement.domain())
                .orElseThrow(() -> new IllegalArgumentException("No data binding for domain: " + requirement.domain()));
        List<String> fields = selectedFields(requirement, binding);
        QueryParts query = buildQuery(requirement, context, binding, fields);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                query.sql(),
                (rs, rowNum) -> mapRow(rs, binding, fields),
                query.args().toArray()
        );
        return new DomainDataset(requirement.domain(), binding.sourceName(), rows);
    }

    private List<String> selectedFields(DataRequirement requirement, DomainBinding binding) {
        Set<String> fields = new LinkedHashSet<>();
        if (requirement.fields() != null && !requirement.fields().isEmpty()) {
            fields.addAll(requirement.fields());
        } else {
            fields.addAll(binding.fieldMappings().keySet());
        }
        fields.removeIf(field -> binding.sourceColumn(field) == null);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("No mapped fields for domain: " + requirement.domain());
        }
        return List.copyOf(fields);
    }

    /**
     * 根据绑定元数据构造单表查询。
     * 该适配器面向常见客户表；复杂关联查询或外部服务调用应实现专用 DomainDataAdapter。
     */
    private QueryParts buildQuery(DataRequirement requirement, QueryContext context, DomainBinding binding, List<String> fields) {
        StringBuilder sql = new StringBuilder("SELECT ");
        for (String field : fields) {
            sql.append(SqlIdentifier.requireSimple(binding.sourceColumn(field))).append(", ");
        }
        sql.setLength(sql.length() - 2);
        sql.append(" FROM ").append(SqlIdentifier.requireQualified(binding.tableName()));
        List<Object> args = new ArrayList<>();
        List<String> predicates = new ArrayList<>();

        predicates.add(SqlIdentifier.requireSimple(binding.clientIdColumn()) + " = ?");
        args.add(context.clientId());

        if (binding.dateColumn() != null && !binding.dateColumn().isBlank()) {
            predicates.add(SqlIdentifier.requireSimple(binding.dateColumn()) + " BETWEEN ? AND ?");
            args.add(Date.valueOf(context.warningStartDate()));
            args.add(Date.valueOf(context.warningEndDate()));
        }

        appendFilters(predicates, args, binding, binding.staticFilters());
        appendFilters(predicates, args, binding, requirement.filters());

        sql.append(" WHERE ").append(String.join(" AND ", predicates));
        if (binding.dateColumn() != null && !binding.dateColumn().isBlank()) {
            sql.append(" ORDER BY ").append(SqlIdentifier.requireSimple(binding.dateColumn()));
        }
        sql.append(" LIMIT ?");
        args.add(Math.max(requirement.sampleLimit(), 1));
        return new QueryParts(sql.toString(), args);
    }

    private void appendFilters(List<String> predicates, List<Object> args, DomainBinding binding, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String sourceColumn = binding.sourceColumn(entry.getKey());
            if (sourceColumn == null) {
                // 忽略未映射的可选过滤字段，使同一规则能在字段略有差异的客户环境中运行。
                continue;
            }
            String column = SqlIdentifier.requireSimple(sourceColumn);
            Object value = entry.getValue();
            if (value instanceof List<?> values && !values.isEmpty()) {
                predicates.add(column + " IN (" + "?,".repeat(values.size()).replaceFirst(",$", "") + ")");
                args.addAll(values);
            } else if (value != null) {
                predicates.add(column + " = ?");
                args.add(value);
            }
        }
    }

    private Map<String, Object> mapRow(ResultSet rs, DomainBinding binding, List<String> fields) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String field : fields) {
            String sourceColumn = binding.sourceColumn(field);
            Object value = rs.getObject(sourceColumn);
            row.put(field, binding.canonicalValue(field, value));
        }
        return row;
    }

    private record QueryParts(String sql, List<Object> args) {
    }
}
