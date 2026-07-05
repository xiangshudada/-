package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.adapter.DomainDataAdapter;
import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.model.DataRequirement;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FundTransferAdapter implements DomainDataAdapter {

    private final JdbcTemplate jdbcTemplate;

    public FundTransferAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean supports(String domain) {
        return "FundTransfer".equals(domain);
    }

    @Override
    public DomainDataset query(DataRequirement requirement, QueryContext context) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                        SELECT transfer_date, client_id, direction, amount, bank_name
                        FROM domain_fund_transfer
                        WHERE client_id = ?
                          AND transfer_date BETWEEN ? AND ?
                        ORDER BY transfer_date
                        LIMIT ?
                        """,
                (rs, rowNum) -> SqlRows.toCamelCaseMap(rs),
                context.clientId(),
                Date.valueOf(context.warningStartDate()),
                Date.valueOf(context.warningEndDate()),
                Math.max(requirement.sampleLimit(), 1)
        );
        return new DomainDataset(requirement.domain(), "domain_fund_transfer", rows);
    }
}
