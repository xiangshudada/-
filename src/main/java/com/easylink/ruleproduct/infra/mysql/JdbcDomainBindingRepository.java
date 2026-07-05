package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.binding.DomainBinding;
import com.easylink.ruleproduct.core.binding.DomainBindingRepository;
import com.easylink.ruleproduct.infra.json.JsonSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDomainBindingRepository implements DomainBindingRepository {

    private static final String DEFAULT_PROFILE = "default";

    private final JdbcTemplate jdbcTemplate;
    private final JsonSupport jsonSupport;

    public JdbcDomainBindingRepository(JdbcTemplate jdbcTemplate, JsonSupport jsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public Optional<DomainBinding> findBinding(String tenantId, String dataSourceProfile, String domain) {
        // 优先使用请求指定的 profile；未配置时回退到 default。
        // 这样同一客户只需要覆盖生产、测试、迁移数据集中确实不同的领域绑定。
        List<BindingRow> rows = jdbcTemplate.query("""
                        SELECT id, tenant_id, data_source_profile, domain, source_name, table_name,
                               client_id_column, date_column, static_filters_json
                        FROM rule_domain_binding
                        WHERE tenant_id = ?
                          AND domain = ?
                          AND enabled = 1
                          AND data_source_profile IN (?, ?)
                        ORDER BY CASE WHEN data_source_profile = ? THEN 0 ELSE 1 END, id
                        LIMIT 1
                        """,
                this::mapBindingRow,
                tenantId,
                domain,
                dataSourceProfile,
                DEFAULT_PROFILE,
                dataSourceProfile
        );
        return rows.stream().findFirst().map(row -> new DomainBinding(
                row.id(),
                row.tenantId(),
                row.dataSourceProfile(),
                row.domain(),
                row.sourceName(),
                row.tableName(),
                row.clientIdColumn(),
                row.dateColumn(),
                loadFieldMappings(row.id()),
                jsonSupport.readMap(row.staticFiltersJson()),
                loadCodeMappings(row.id())
        ));
    }

    private Map<String, String> loadFieldMappings(Long bindingId) {
        return jdbcTemplate.query("""
                        SELECT canonical_field, source_column
                        FROM rule_field_mapping
                        WHERE binding_id = ?
                        ORDER BY id
                        """,
                rs -> {
                    Map<String, String> mappings = new LinkedHashMap<>();
                    while (rs.next()) {
                        mappings.put(rs.getString("canonical_field"), rs.getString("source_column"));
                    }
                    return mappings;
                },
                bindingId
        );
    }

    private Map<String, Map<String, String>> loadCodeMappings(Long bindingId) {
        return jdbcTemplate.query("""
                        SELECT canonical_field, source_value, canonical_value
                        FROM rule_code_mapping
                        WHERE binding_id = ?
                        ORDER BY id
                        """,
                rs -> {
                    Map<String, Map<String, String>> mappings = new LinkedHashMap<>();
                    while (rs.next()) {
                        mappings.computeIfAbsent(rs.getString("canonical_field"), key -> new LinkedHashMap<>())
                                .put(rs.getString("source_value"), rs.getString("canonical_value"));
                    }
                    return mappings;
                },
                bindingId
        );
    }

    private BindingRow mapBindingRow(ResultSet rs, int rowNum) throws SQLException {
        return new BindingRow(
                rs.getLong("id"),
                rs.getString("tenant_id"),
                rs.getString("data_source_profile"),
                rs.getString("domain"),
                rs.getString("source_name"),
                rs.getString("table_name"),
                rs.getString("client_id_column"),
                rs.getString("date_column"),
                rs.getString("static_filters_json")
        );
    }

    private record BindingRow(
            Long id,
            String tenantId,
            String dataSourceProfile,
            String domain,
            String sourceName,
            String tableName,
            String clientIdColumn,
            String dateColumn,
            String staticFiltersJson
    ) {
    }
}
