package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.adapter.DomainDataAdapter;
import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.model.DataRequirement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerProfileAdapter implements DomainDataAdapter {

    private final JdbcTemplate jdbcTemplate;

    public CustomerProfileAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean supports(String domain) {
        return "CustomerProfile".equals(domain);
    }

    @Override
    public DomainDataset query(DataRequirement requirement, QueryContext context) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.query("""
                        SELECT client_id, client_type, age, occupation, annual_income, registered_capital
                        FROM domain_customer_profile
                        WHERE client_id = ?
                        """,
                (rs, rowNum) -> SqlRows.toCamelCaseMap(rs),
                context.clientId()
        );
        return new DomainDataset(requirement.domain(), "domain_customer_profile", rows);
    }
}
