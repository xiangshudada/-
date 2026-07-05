package com.easylink.ruleproduct.infra.mysql;

import com.easylink.ruleproduct.core.model.DataRequirement;
import com.easylink.ruleproduct.core.model.ClientType;
import com.easylink.ruleproduct.core.model.DataGrain;
import com.easylink.ruleproduct.core.model.DecisionPolicy;
import com.easylink.ruleproduct.core.model.IndicatorModel;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.MissingDataPolicy;
import com.easylink.ruleproduct.core.model.OperatorType;
import com.easylink.ruleproduct.core.model.ReportMapping;
import com.easylink.ruleproduct.core.model.RequirementScope;
import com.easylink.ruleproduct.core.model.RiskSection;
import com.easylink.ruleproduct.core.model.ThresholdProfile;
import com.easylink.ruleproduct.core.model.VerificationItem;
import com.easylink.ruleproduct.core.repository.IndicatorModelRepository;
import com.easylink.ruleproduct.infra.json.JsonSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIndicatorModelRepository implements IndicatorModelRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonSupport jsonSupport;

    public JdbcIndicatorModelRepository(JdbcTemplate jdbcTemplate, JsonSupport jsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public IndicatorModel loadModel(String indicatorCode) {
        IndicatorRow indicator = jdbcTemplate.query("""
                        SELECT indicator_code, indicator_name, scenario, rule_version
                        FROM rule_indicator_set
                        WHERE indicator_code = ? AND enabled = 1
                        """,
                this::mapIndicator,
                indicatorCode
        ).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + indicatorCode));

        List<RiskSection> sections = jdbcTemplate.query("""
                        SELECT id, risk_type, crime_type, risk_point, section_order
                        FROM rule_risk_section
                        WHERE indicator_code = ?
                        ORDER BY section_order, id
                        """,
                (rs, rowNum) -> mapSection(rs, indicatorCode),
                indicatorCode
        );
        return new IndicatorModel(
                indicator.indicatorCode(),
                indicator.indicatorName(),
                indicator.scenario(),
                indicator.ruleVersion(),
                sections
        );
    }

    private RiskSection mapSection(ResultSet rs, String indicatorCode) throws SQLException {
        Long sectionId = rs.getLong("id");
        List<InvestigationStep> steps = jdbcTemplate.query("""
                        SELECT id, step_code, step_name, step_order, method_text, rule_text, operator_type,
                               applicable_client_types, thresholds_json, judgment_basis, manual_review_required,
                               report_field, result_target
                        FROM rule_investigation_step
                        WHERE section_id = ?
                        ORDER BY step_order, id
                        """,
                (stepRs, rowNum) -> mapStep(stepRs, indicatorCode),
                sectionId
        );
        return new RiskSection(
                sectionId,
                rs.getString("risk_type"),
                rs.getString("crime_type"),
                rs.getString("risk_point"),
                rs.getInt("section_order"),
                steps
        );
    }

    private InvestigationStep mapStep(ResultSet rs, String indicatorCode) throws SQLException {
        Long stepId = rs.getLong("id");
        ThresholdProfile thresholdProfile = thresholdProfile(rs.getString("thresholds_json"));
        List<VerificationItem> verificationItems = loadVerificationItems(stepId);
        List<DataRequirement> dataRequirements = loadRequirements(stepId);
        DecisionPolicy decisionPolicy = new DecisionPolicy(rs.getString("judgment_basis"), rs.getBoolean("manual_review_required"));
        ReportMapping reportMapping = new ReportMapping(rs.getString("report_field"), rs.getString("result_target"));
        if (verificationItems.isEmpty()) {
            return new InvestigationStep(
                    stepId,
                    rs.getString("step_code"),
                    rs.getString("step_name"),
                    rs.getInt("step_order"),
                    rs.getString("method_text"),
                    rs.getString("rule_text"),
                    OperatorType.valueOf(rs.getString("operator_type")),
                    ClientType.parseCsv(rs.getString("applicable_client_types")),
                    dataRequirements,
                    thresholdProfile,
                    decisionPolicy,
                    reportMapping
            );
        }
        return new InvestigationStep(
                stepId,
                rs.getString("step_code"),
                rs.getString("step_name"),
                rs.getInt("step_order"),
                rs.getString("method_text"),
                rs.getString("rule_text"),
                OperatorType.valueOf(rs.getString("operator_type")),
                ClientType.parseCsv(rs.getString("applicable_client_types")),
                dataRequirements,
                thresholdProfile,
                decisionPolicy,
                reportMapping,
                verificationItems
        );
    }

    private List<VerificationItem> loadVerificationItems(Long stepId) {
        return jdbcTemplate.query("""
                        SELECT id, item_code, item_name, item_order, method_text, rule_text, operator_type,
                               applicable_client_types, thresholds_json, judgment_basis, manual_review_required
                        FROM rule_verification_item
                        WHERE step_id = ?
                        ORDER BY item_order, id
                        """,
                (rs, rowNum) -> {
                    Long itemId = rs.getLong("id");
                    return new VerificationItem(
                            itemId,
                            rs.getString("item_code"),
                            rs.getString("item_name"),
                            rs.getInt("item_order"),
                            rs.getString("method_text"),
                            rs.getString("rule_text"),
                            OperatorType.valueOf(rs.getString("operator_type")),
                            ClientType.parseCsv(rs.getString("applicable_client_types")),
                            loadVerificationItemRequirements(itemId),
                            thresholdProfile(rs.getString("thresholds_json")),
                            new DecisionPolicy(rs.getString("judgment_basis"), rs.getBoolean("manual_review_required"))
                    );
                },
                stepId
        );
    }

    private ThresholdProfile thresholdProfile(String thresholdsJson) {
        Map<String, Object> thresholdMap = jsonSupport.readMap(thresholdsJson);
        return new ThresholdProfile(
                (String) thresholdMap.get("metric"),
                jsonSupport.decimalValue(thresholdMap, "min"),
                jsonSupport.decimalValue(thresholdMap, "max"),
                thresholdMap
        );
    }

    private List<DataRequirement> loadRequirements(Long stepId) {
        return jdbcTemplate.query("""
                        SELECT id, requirement_code, domain, scope_type, grain, filters_json, fields_json,
                               sample_limit, missing_data_policy
                        FROM rule_data_requirement
                        WHERE step_id = ?
                        ORDER BY id
                        """,
                this::mapRequirement,
                stepId
        );
    }

    private List<DataRequirement> loadVerificationItemRequirements(Long itemId) {
        return jdbcTemplate.query("""
                        SELECT id, requirement_code, domain, scope_type, grain, filters_json, fields_json,
                               sample_limit, missing_data_policy
                        FROM rule_verification_data_requirement
                        WHERE verification_item_id = ?
                        ORDER BY id
                        """,
                this::mapRequirement,
                itemId
        );
    }

    private DataRequirement mapRequirement(ResultSet rs, int rowNum) throws SQLException {
        return new DataRequirement(
                rs.getLong("id"),
                rs.getString("requirement_code"),
                rs.getString("domain"),
                RequirementScope.fromCode(rs.getString("scope_type")),
                DataGrain.fromCode(rs.getString("grain")),
                jsonSupport.readMap(rs.getString("filters_json")),
                jsonSupport.readStringList(rs.getString("fields_json")),
                rs.getInt("sample_limit"),
                MissingDataPolicy.fromCode(rs.getString("missing_data_policy"))
        );
    }

    private IndicatorRow mapIndicator(ResultSet rs, int rowNum) throws SQLException {
        return new IndicatorRow(
                rs.getString("indicator_code"),
                rs.getString("indicator_name"),
                rs.getString("scenario"),
                rs.getString("rule_version")
        );
    }

    private record IndicatorRow(String indicatorCode, String indicatorName, String scenario, String ruleVersion) {
    }
}
