package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.model.ExecutionResult;
import com.easylink.ruleproduct.core.model.StepExecutionResult;
import com.easylink.ruleproduct.core.repository.ExecutionResultRepository;
import com.easylink.ruleproduct.infra.json.JsonSupport;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcExecutionResultRepository implements ExecutionResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonSupport jsonSupport;

    public JdbcExecutionResultRepository(JdbcTemplate jdbcTemplate, JsonSupport jsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = jsonSupport;
    }

    @Override
    @Transactional
    public void save(ExecutionResult result) {
        jdbcTemplate.update("""
                        INSERT INTO rule_execution
                            (execution_id, tenant_id, data_source_profile, indicator_code, client_id,
                             alert_date, warning_start_date, warning_end_date, executed_at, report_draft_json)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                result.executionId(),
                result.tenantId(),
                result.dataSourceProfile(),
                result.indicatorCode(),
                result.clientId(),
                result.alertDate(),
                result.warningStartDate(),
                result.warningEndDate(),
                Timestamp.valueOf(result.executedAt()),
                jsonSupport.write(result.reportDraft())
        );
        for (StepExecutionResult step : result.steps()) {
            jdbcTemplate.update("""
                            INSERT INTO rule_step_execution
                                (execution_id, step_id, step_code, step_name, operator_type, decision_status,
                                 decision_summary, decision_details_json, facts_json, report_field)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    result.executionId(),
                    step.stepId(),
                    step.stepCode(),
                    step.stepName(),
                    step.operatorType().name(),
                    step.decision().status().name(),
                    step.decision().summary(),
                    jsonSupport.write(step.decision().details()),
                    jsonSupport.write(step.facts()),
                    step.reportMapping() == null ? null : step.reportMapping().reportField()
            );
        }
    }
}
