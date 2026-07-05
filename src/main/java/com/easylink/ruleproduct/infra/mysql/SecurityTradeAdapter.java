package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.adapter.DomainDataAdapter;
import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.model.DataRequirement;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SecurityTradeAdapter implements DomainDataAdapter {

    private final JdbcTemplate jdbcTemplate;

    public SecurityTradeAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean supports(String domain) {
        return "SecurityTrade".equals(domain) || "MarginTrade".equals(domain) || "BlockTrade".equals(domain);
    }

    @Override
    public DomainDataset query(DataRequirement requirement, QueryContext context) {
        StringBuilder sql = new StringBuilder("""
                SELECT trade_date, trade_time, client_id, security_code, security_name, direction,
                       business_flag, trade_mode, price, quantity, amount
                FROM domain_security_trade
                WHERE client_id = ?
                  AND trade_date BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(context.clientId());
        args.add(Date.valueOf(context.warningStartDate()));
        args.add(Date.valueOf(context.warningEndDate()));

        Object businessFlags = requirement.filters().get("businessFlags");
        if (businessFlags instanceof List<?> flags && !flags.isEmpty()) {
            sql.append(" AND business_flag IN (");
            sql.append("?,".repeat(flags.size()));
            sql.setLength(sql.length() - 1);
            sql.append(")");
            args.addAll(flags);
        }

        Object tradeMode = requirement.filters().get("tradeMode");
        if (tradeMode instanceof String mode && !mode.isBlank()) {
            sql.append(" AND trade_mode = ?");
            args.add(mode);
        }

        sql.append(" ORDER BY trade_date, trade_time, security_code LIMIT ?");
        args.add(Math.max(requirement.sampleLimit(), 1));

        List<Map<String, Object>> rows = jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> SqlRows.toCamelCaseMap(rs),
                args.toArray()
        );
        return new DomainDataset(requirement.domain(), "domain_security_trade", rows);
    }
}
