package com.easylink.ruleproduct.infra.mysql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.binding.DomainBinding;
import com.easylink.ruleproduct.core.binding.DomainBindingRepository;
import com.easylink.ruleproduct.core.model.DataGrain;
import com.easylink.ruleproduct.core.model.DataRequirement;
import com.easylink.ruleproduct.core.model.MissingDataPolicy;
import com.easylink.ruleproduct.core.model.RequirementScope;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ConfigurableJdbcDomainDataAdapterTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldProjectTenantColumnsToCanonicalDataset() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
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
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            sql.set(invocation.getArgument(0));
            args.set(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
            RowMapper<Map<String, Object>> mapper = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getObject("cust_no")).thenReturn("C001");
            when(rs.getObject("init_date")).thenReturn(java.sql.Date.valueOf("2026-06-24"));
            when(rs.getObject("bs_flag")).thenReturn("1");
            when(rs.getObject("match_amt")).thenReturn(new BigDecimal("1200.00"));
            return List.of(mapper.mapRow(rs, 0));
        });

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
}
