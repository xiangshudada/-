package com.easylink.ruleproduct.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.easylink.ruleproduct.api.dto.RuleExecutionRequest;
import com.easylink.ruleproduct.core.adapter.DomainDataAdapter;
import com.easylink.ruleproduct.core.adapter.DomainDataAdapterRegistry;
import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.model.DataRequirement;
import com.easylink.ruleproduct.core.model.ClientType;
import com.easylink.ruleproduct.core.model.DataGrain;
import com.easylink.ruleproduct.core.model.DecisionPolicy;
import com.easylink.ruleproduct.core.model.ExecutionResult;
import com.easylink.ruleproduct.core.model.IndicatorModel;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.MissingDataPolicy;
import com.easylink.ruleproduct.core.model.OperatorType;
import com.easylink.ruleproduct.core.model.ReportMapping;
import com.easylink.ruleproduct.core.model.RequirementScope;
import com.easylink.ruleproduct.core.model.RiskSection;
import com.easylink.ruleproduct.core.model.ThresholdProfile;
import com.easylink.ruleproduct.core.model.VerificationItem;
import com.easylink.ruleproduct.core.operator.ObjectiveDescribeOperator;
import com.easylink.ruleproduct.core.operator.RuleOperatorRegistry;
import com.easylink.ruleproduct.core.operator.ThresholdCompareOperator;
import com.easylink.ruleproduct.core.repository.ExecutionResultRepository;
import com.easylink.ruleproduct.core.repository.IndicatorModelRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RuleExecutionServiceTest {

    @Test
    void shouldExecuteAndPersistResultWithTenantContext() {
        IndicatorModelRepository modelRepository = indicatorCode -> new IndicatorModel(
                indicatorCode,
                "sample indicator",
                "SAMPLE",
                "v1",
                List.of(new RiskSection(
                        1L,
                        "基础调查",
                        "-",
                        "确认交易事实",
                        1,
                        List.of(new InvestigationStep(
                                11L,
                                "STEP_FACT",
                                "确认事实",
                                1,
                                "查看交易",
                                "客观陈述",
                                OperatorType.OBJECTIVE_DESCRIBE,
                                Set.of(ClientType.PERSON),
                                List.of(new DataRequirement(
                                        101L,
                                        "REQ_TRADE",
                                        "SecurityTrade",
                                        RequirementScope.WARNING_PERIOD,
                                        DataGrain.DETAIL,
                                        Map.of(),
                                        List.of("amount"),
                                        20,
                                        MissingDataPolicy.RETURN_EMPTY_WITH_REASON
                                )),
                                new ThresholdProfile(null, null, null, Map.of()),
                                new DecisionPolicy("客观陈述", false),
                                new ReportMapping("可疑行为描述", null)
                        ))
                ))
        );
        DomainDataAdapter adapter = new DomainDataAdapter() {
            @Override
            public boolean supports(String domain) {
                return "SecurityTrade".equals(domain);
            }

            @Override
            public DomainDataset query(DataRequirement requirement, QueryContext context) {
                return new DomainDataset(
                        requirement.domain(),
                        context.tenantId(),
                        List.of(Map.of("amount", new BigDecimal("1000.00")))
                );
            }
        };
        AtomicReference<ExecutionResult> saved = new AtomicReference<>();
        ExecutionResultRepository resultRepository = saved::set;
        RuleExecutionService service = new RuleExecutionService(
                modelRepository,
                new DomainDataAdapterRegistry(List.of(adapter)),
                new RuleOperatorRegistry(List.of(new ObjectiveDescribeOperator())),
                new FactFactory(),
                resultRepository
        );
        RuleExecutionRequest request = new RuleExecutionRequest(
                "tenant-a",
                "prod",
                "1214_1001",
                "C001",
                LocalDate.parse("2026-06-24"),
                null,
                null
        );

        ExecutionResult result = service.execute(request);

        assertThat(saved.get()).isSameAs(result);
        assertThat(result.tenantId()).isEqualTo("tenant-a");
        assertThat(result.dataSourceProfile()).isEqualTo("prod");
        assertThat(result.warningStartDate()).isEqualTo(LocalDate.parse("2026-05-25"));
        assertThat(result.reportDraft()).containsKey("可疑行为描述");
    }

    @Test
    void shouldPreloadSectionDataOnceForSharedVerificationRequirements() {
        DataRequirement requirementOne = new DataRequirement(
                101L,
                "REQ_TRADE_ONE",
                "SecurityTrade",
                RequirementScope.WARNING_PERIOD,
                DataGrain.DETAIL,
                Map.of(),
                List.of("amount"),
                1,
                MissingDataPolicy.RETURN_EMPTY_WITH_REASON
        );
        DataRequirement requirementTwo = new DataRequirement(
                102L,
                "REQ_TRADE_TWO",
                "SecurityTrade",
                RequirementScope.WARNING_PERIOD,
                DataGrain.DETAIL,
                Map.of(),
                List.of("amount"),
                20,
                MissingDataPolicy.RETURN_EMPTY_WITH_REASON
        );
        VerificationItem itemOne = new VerificationItem(
                201L,
                "ITEM_FACT_ONE",
                "确认交易事实一",
                1,
                "查看交易一",
                "客观陈述",
                OperatorType.OBJECTIVE_DESCRIBE,
                Set.of(ClientType.PERSON),
                List.of(requirementOne),
                ThresholdProfile.empty(),
                new DecisionPolicy("客观陈述", false)
        );
        VerificationItem itemTwo = new VerificationItem(
                202L,
                "ITEM_FACT_TWO",
                "确认交易事实二",
                2,
                "查看交易二",
                "客观陈述",
                OperatorType.OBJECTIVE_DESCRIBE,
                Set.of(ClientType.PERSON),
                List.of(requirementTwo),
                ThresholdProfile.empty(),
                new DecisionPolicy("客观陈述", false)
        );
        IndicatorModelRepository modelRepository = indicatorCode -> new IndicatorModel(
                indicatorCode,
                "sample indicator",
                "SAMPLE",
                "v1",
                List.of(new RiskSection(
                        1L,
                        "基础调查",
                        "-",
                        "确认交易事实",
                        1,
                        List.of(new InvestigationStep(
                                11L,
                                "STEP_FACT",
                                "确认事实",
                                1,
                                "查看交易",
                                "客观陈述",
                                OperatorType.OBJECTIVE_DESCRIBE,
                                Set.of(ClientType.PERSON),
                                List.of(),
                                ThresholdProfile.empty(),
                                new DecisionPolicy("客观陈述", false),
                                new ReportMapping("可疑行为描述", null),
                                List.of(itemOne, itemTwo)
                        ))
                ))
        );
        AtomicInteger queryCount = new AtomicInteger();
        DomainDataAdapter adapter = new DomainDataAdapter() {
            @Override
            public boolean supports(String domain) {
                return "SecurityTrade".equals(domain);
            }

            @Override
            public DomainDataset query(DataRequirement requirement, QueryContext context) {
                queryCount.incrementAndGet();
                assertThat(requirement.sampleLimit()).isEqualTo(20);
                return new DomainDataset(
                        requirement.domain(),
                        context.tenantId(),
                        List.of(
                                Map.of("amount", new BigDecimal("1000.00")),
                                Map.of("amount", new BigDecimal("2000.00"))
                        )
                );
            }
        };
        RuleExecutionService service = new RuleExecutionService(
                modelRepository,
                new DomainDataAdapterRegistry(List.of(adapter)),
                new RuleOperatorRegistry(List.of(new ObjectiveDescribeOperator())),
                new FactFactory(),
                result -> {
                }
        );

        ExecutionResult result = service.execute(new RuleExecutionRequest(
                "tenant-a",
                "prod",
                "1214_1001",
                "C001",
                LocalDate.parse("2026-06-24"),
                null,
                null
        ));

        assertThat(queryCount).hasValue(1);
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).verificationResults()).hasSize(2);
        assertThat(result.steps().get(0).verificationResults().get(0).facts().get(0).samples()).hasSize(1);
        assertThat(result.steps().get(0).verificationResults().get(1).facts().get(0).samples()).hasSize(2);
    }

    @Test
    void shouldSkipVerificationItemWhenClientTypeDoesNotApply() {
        DataRequirement profileRequirement = new DataRequirement(
                101L,
                "REQ_PROFILE",
                "CustomerProfile",
                RequirementScope.ALERT_DETAIL,
                DataGrain.ACCOUNT_SUMMARY,
                Map.of(),
                List.of("clientType", "annualIncome"),
                1,
                MissingDataPolicy.RETURN_EMPTY_WITH_REASON
        );
        VerificationItem personItem = new VerificationItem(
                201L,
                "ITEM_PERSON",
                "个人收入核验",
                1,
                "",
                "",
                OperatorType.THRESHOLD_COMPARE,
                Set.of(ClientType.PERSON),
                List.of(profileRequirement),
                new ThresholdProfile("annualIncome", new BigDecimal("100000"), null, Map.of()),
                new DecisionPolicy("", false)
        );
        VerificationItem orgItem = new VerificationItem(
                202L,
                "ITEM_ORG",
                "机构注册资本核验",
                2,
                "",
                "",
                OperatorType.THRESHOLD_COMPARE,
                Set.of(ClientType.ORG),
                List.of(profileRequirement),
                new ThresholdProfile("registeredCapital", new BigDecimal("1000000"), null, Map.of()),
                new DecisionPolicy("", false)
        );
        IndicatorModelRepository modelRepository = indicatorCode -> new IndicatorModel(
                indicatorCode,
                "sample indicator",
                "SAMPLE",
                "v1",
                List.of(new RiskSection(
                        1L,
                        "基础调查",
                        "-",
                        "客户类型过滤",
                        1,
                        List.of(new InvestigationStep(
                                11L,
                                "STEP_PROFILE",
                                "客户画像核验",
                                1,
                                "",
                                "",
                                OperatorType.THRESHOLD_COMPARE,
                                Set.of(ClientType.PERSON, ClientType.ORG),
                                List.of(),
                                ThresholdProfile.empty(),
                                new DecisionPolicy("", false),
                                new ReportMapping("疑点分析", null),
                                List.of(personItem, orgItem)
                        ))
                ))
        );
        DomainDataAdapter adapter = new DomainDataAdapter() {
            @Override
            public boolean supports(String domain) {
                return "CustomerProfile".equals(domain);
            }

            @Override
            public DomainDataset query(DataRequirement requirement, QueryContext context) {
                return new DomainDataset(
                        requirement.domain(),
                        context.tenantId(),
                        List.of(Map.of("clientType", "PERSON", "annualIncome", new BigDecimal("200000")))
                );
            }
        };
        RuleExecutionService service = new RuleExecutionService(
                modelRepository,
                new DomainDataAdapterRegistry(List.of(adapter)),
                new RuleOperatorRegistry(List.of(new ThresholdCompareOperator())),
                new FactFactory(),
                result -> {
                }
        );

        ExecutionResult result = service.execute(new RuleExecutionRequest(
                "tenant-a",
                "prod",
                "1214_1001",
                "C001",
                LocalDate.parse("2026-06-24"),
                null,
                null
        ));

        assertThat(result.steps().get(0).verificationResults()).hasSize(2);
        assertThat(result.steps().get(0).verificationResults().get(0).decision().status()).isEqualTo(com.easylink.ruleproduct.core.model.DecisionStatus.HIT);
        assertThat(result.steps().get(0).verificationResults().get(1).decision().status()).isEqualTo(com.easylink.ruleproduct.core.model.DecisionStatus.NOT_HIT);
        assertThat(result.steps().get(0).verificationResults().get(1).decision().summary()).contains("不适用");
    }
}
