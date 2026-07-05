package com.easylink.ruleproduct.infra.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.binding.DomainBinding;
import com.easylink.ruleproduct.core.binding.DomainBindingRepository;
import com.easylink.ruleproduct.core.model.DataGrain;
import com.easylink.ruleproduct.core.model.DataRequirement;
import com.easylink.ruleproduct.core.model.MissingDataPolicy;
import com.easylink.ruleproduct.core.model.RequirementScope;
import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ConfigurableJdbcDomainDataAdapterTest {

    @Test
    void shouldProjectTenantColumnsToCanonicalDataset() {
        DomainBindingRepository repository = (tenantId, profile, domain) -> Optional.of(new DomainBinding(
                1L,
                tenantId,
                profile,
                domain,
                "customer-a",
                "trade_confirm",
                "cust_no",
                "init_date",
                Map.of(
                        "clientId", "cust_no",
                        "tradeDate", "init_date",
                        "direction", "bs_flag",
                        "amount", "match_amt",
                        "businessFlags", "business_flag"
                ),
                Map.of(),
                Map.of("direction", Map.of("1", "BUY"))
        ));
        AtomicReference<String> sql = new AtomicReference<>();
        AtomicReference<Object[]> args = new AtomicReference<>();
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String querySql, RowMapper<T> rowMapper, Object... queryArgs) {
                sql.set(querySql);
                args.set(queryArgs);
                try {
                    ResultSet rs = resultSet(Map.of(
                            "cust_no", "C001",
                            "init_date", java.sql.Date.valueOf("2026-06-24"),
                            "bs_flag", "1",
                            "match_amt", new BigDecimal("1200.00")
                    ));
                    return List.of(rowMapper.mapRow(rs, 0));
                } catch (SQLException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        };

        ConfigurableJdbcDomainDataAdapter adapter = new ConfigurableJdbcDomainDataAdapter(jdbcTemplate, repository);
        DataRequirement requirement = new DataRequirement(
                1L,
                "REQ",
                "SecurityTrade",
                RequirementScope.WARNING_PERIOD,
                DataGrain.DETAIL,
                Map.of("businessFlags", List.of(4211, 4212)),
                List.of("clientId", "tradeDate", "direction", "amount"),
                20,
                MissingDataPolicy.RETURN_EMPTY_WITH_REASON
        );
        QueryContext context = new QueryContext(
                "tenant-a",
                "prod",
                "1214_1001",
                "C001",
                LocalDate.parse("2026-06-24"),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30")
        );

        DomainDataset dataset = adapter.query(requirement, context);

        assertThat(sql.get()).contains("FROM trade_confirm");
        assertThat(sql.get()).contains("cust_no = ?");
        assertThat(sql.get()).contains("init_date BETWEEN ? AND ?");
        assertThat(sql.get()).contains("business_flag IN (?,?)");
        assertThat(args.get()).containsExactly(
                "C001",
                java.sql.Date.valueOf("2026-06-01"),
                java.sql.Date.valueOf("2026-06-30"),
                4211,
                4212,
                20
        );
        assertThat(dataset.rows()).singleElement().satisfies(row -> {
            assertThat(row.get("clientId")).isEqualTo("C001");
            assertThat(row.get("direction")).isEqualTo("BUY");
            assertThat(row.get("amount")).isEqualTo(new BigDecimal("1200.00"));
        });
    }

    @Test
    void shouldRejectUnsafeIdentifiers() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> SqlIdentifier.requireQualified("trade;drop"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ResultSet resultSet(Map<String, Object> values) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    if ("getObject".equals(method.getName())) {
                        return values.get((String) args[0]);
                    }
                    if ("wasNull".equals(method.getName())) {
                        return false;
                    }
                    if ("toString".equals(method.getName())) {
                        return values.toString();
                    }
                    throw new UnsupportedOperationException("Unsupported ResultSet method: " + method.getName());
                }
        );
    }
}
