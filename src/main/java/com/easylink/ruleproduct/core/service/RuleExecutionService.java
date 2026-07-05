package com.easylink.ruleproduct.core.service;

import com.easylink.ruleproduct.api.dto.RuleExecutionRequest;
import com.easylink.ruleproduct.core.adapter.DomainDataAdapterRegistry;
import com.easylink.ruleproduct.core.adapter.DomainDataset;
import com.easylink.ruleproduct.core.adapter.QueryContext;
import com.easylink.ruleproduct.core.model.DataGrain;
import com.easylink.ruleproduct.core.model.DataRequirement;
import com.easylink.ruleproduct.core.model.DecisionResult;
import com.easylink.ruleproduct.core.model.DecisionStatus;
import com.easylink.ruleproduct.core.model.ExecutionResult;
import com.easylink.ruleproduct.core.model.Fact;
import com.easylink.ruleproduct.core.model.IndicatorModel;
import com.easylink.ruleproduct.core.model.InvestigationStep;
import com.easylink.ruleproduct.core.model.ReportMapping;
import com.easylink.ruleproduct.core.model.RequirementScope;
import com.easylink.ruleproduct.core.model.RiskSection;
import com.easylink.ruleproduct.core.model.StepExecutionResult;
import com.easylink.ruleproduct.core.model.VerificationExecutionResult;
import com.easylink.ruleproduct.core.model.VerificationItem;
import com.easylink.ruleproduct.core.operator.RuleOperatorRegistry;
import com.easylink.ruleproduct.core.repository.IndicatorModelRepository;
import com.easylink.ruleproduct.core.repository.ExecutionResultRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RuleExecutionService {

    private final IndicatorModelRepository indicatorModelRepository;
    private final DomainDataAdapterRegistry adapterRegistry;
    private final RuleOperatorRegistry operatorRegistry;
    private final FactFactory factFactory;
    private final ExecutionResultRepository executionResultRepository;

    public RuleExecutionService(
            IndicatorModelRepository indicatorModelRepository,
            DomainDataAdapterRegistry adapterRegistry,
            RuleOperatorRegistry operatorRegistry,
            FactFactory factFactory,
            ExecutionResultRepository executionResultRepository
    ) {
        this.indicatorModelRepository = indicatorModelRepository;
        this.adapterRegistry = adapterRegistry;
        this.operatorRegistry = operatorRegistry;
        this.factFactory = factFactory;
        this.executionResultRepository = executionResultRepository;
    }

    /**
     * 对单个客户执行单个指标。
     *
     * 该服务只依赖抽象仓储、领域数据适配器和规则算子。
     * 租户特定的表名、字段名和码值必须在 DomainDataset 之前完成解析；
     * 本方法不应出现客户专属 SQL 或客户分支逻辑。
     */
    public ExecutionResult execute(RuleExecutionRequest request) {
        IndicatorModel model = indicatorModelRepository.loadModel(request.indicatorCode());
        QueryContext context = buildContext(request);
        List<StepExecutionResult> stepResults = new ArrayList<>();
        Map<String, List<String>> reportDraft = new LinkedHashMap<>();

        for (RiskSection section : model.sections()) {
            SectionDataContext sectionData = preloadSectionData(section, context);
            for (InvestigationStep step : section.steps()) {
                List<VerificationExecutionResult> verificationResults = executeVerificationItems(step, sectionData);
                DecisionResult decision = aggregateStepDecision(step, verificationResults);
                List<Fact> facts = verificationResults.stream()
                        .flatMap(result -> result.facts().stream())
                        .toList();
                StepExecutionResult stepResult = new StepExecutionResult(
                        step.stepId(),
                        step.stepCode(),
                        step.stepName(),
                        step.operatorType(),
                        facts,
                        decision,
                        step.reportMapping(),
                        verificationResults
                );
                stepResults.add(stepResult);
                appendReport(reportDraft, step.reportMapping(), decision.summary());
            }
        }

        ExecutionResult result = new ExecutionResult(
                UUID.randomUUID().toString(),
                context.tenantId(),
                context.dataSourceProfile(),
                request.indicatorCode(),
                request.clientId(),
                request.alertDate(),
                context.warningStartDate(),
                context.warningEndDate(),
                LocalDateTime.now(),
                stepResults,
                reportDraft
        );
        executionResultRepository.save(result);
        return result;
    }

    private QueryContext buildContext(RuleExecutionRequest request) {
        // 默认以预警日期为右边界，向前取 30 天作为预警窗口。
        // 单个 DataRequirement 仍可通过自身配置追加更窄的过滤条件。
        LocalDate warningStart = request.warningStartDate() == null
                ? request.alertDate().minusDays(30)
                : request.warningStartDate();
        LocalDate warningEnd = request.warningEndDate() == null
                ? request.alertDate()
                : request.warningEndDate();
        return new QueryContext(
                request.tenantIdOrDefault(),
                request.dataSourceProfileOrDefault(),
                request.indicatorCode(),
                request.clientId(),
                request.alertDate(),
                warningStart,
                warningEnd
        );
    }

    private SectionDataContext preloadSectionData(RiskSection section, QueryContext context) {
        Map<RequirementKey, DataRequirement> requirements = new LinkedHashMap<>();
        for (InvestigationStep step : section.steps()) {
            for (VerificationItem item : step.executableItems()) {
                for (DataRequirement requirement : item.dataRequirements()) {
                    RequirementKey key = RequirementKey.from(requirement);
                    requirements.merge(key, requirement, this::maxSampleLimitRequirement);
                }
            }
        }

        Map<RequirementKey, Fact> facts = new HashMap<>();
        for (Map.Entry<RequirementKey, DataRequirement> entry : requirements.entrySet()) {
            DataRequirement requirement = entry.getValue();
            DomainDataset dataset = adapterRegistry.find(requirement.domain(), context).query(requirement, context);
            facts.put(entry.getKey(), factFactory.fromDataset(dataset, requirement.sampleLimit()));
        }
        return new SectionDataContext(facts);
    }

    private DataRequirement maxSampleLimitRequirement(DataRequirement left, DataRequirement right) {
        return left.sampleLimit() >= right.sampleLimit() ? left : right;
    }

    private List<VerificationExecutionResult> executeVerificationItems(InvestigationStep step, SectionDataContext sectionData) {
        List<VerificationExecutionResult> results = new ArrayList<>();
        for (VerificationItem item : step.executableItems()) {
            List<Fact> facts = item.dataRequirements().stream()
                    .map(requirement -> sectionData.factFor(requirement))
                    .toList();
            InvestigationStep itemStep = step.viewFor(item);
            DecisionResult decision = operatorRegistry.get(item.operatorType()).evaluate(itemStep, facts);
            results.add(new VerificationExecutionResult(
                    item.itemId(),
                    item.itemCode(),
                    item.itemName(),
                    item.operatorType(),
                    facts,
                    decision
            ));
        }
        return results;
    }

    private DecisionResult aggregateStepDecision(InvestigationStep step, List<VerificationExecutionResult> results) {
        if (results.isEmpty()) {
            return new DecisionResult(DecisionStatus.DATA_MISSING, "步骤未配置核验项，无法判断。", Map.of());
        }
        if (results.size() == 1) {
            return results.get(0).decision();
        }

        DecisionStatus status = aggregateStatus(results);
        String summary = step.stepName() + "：共执行 " + results.size() + " 个核验项，"
                + results.stream()
                .map(result -> result.itemName() + "[" + result.decision().status() + "]")
                .reduce((left, right) -> left + "；" + right)
                .orElse("无核验结果")
                + "。";
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("itemCount", results.size());
        details.put("hitCount", results.stream().filter(result -> result.decision().status() == DecisionStatus.HIT).count());
        details.put("pendingManualCount", results.stream().filter(result -> result.decision().status() == DecisionStatus.PENDING_MANUAL).count());
        details.put("dataMissingCount", results.stream().filter(result -> result.decision().status() == DecisionStatus.DATA_MISSING).count());
        return new DecisionResult(status, summary, details);
    }

    private DecisionStatus aggregateStatus(List<VerificationExecutionResult> results) {
        if (results.stream().anyMatch(result -> result.decision().status() == DecisionStatus.HIT)) {
            return DecisionStatus.HIT;
        }
        if (results.stream().anyMatch(result -> result.decision().status() == DecisionStatus.PENDING_MANUAL)) {
            return DecisionStatus.PENDING_MANUAL;
        }
        if (results.stream().anyMatch(result -> result.decision().status() == DecisionStatus.DATA_MISSING)) {
            return DecisionStatus.DATA_MISSING;
        }
        if (results.stream().allMatch(result -> result.decision().status() == DecisionStatus.NOT_HIT)) {
            return DecisionStatus.NOT_HIT;
        }
        return DecisionStatus.NEUTRAL;
    }

    private void appendReport(Map<String, List<String>> reportDraft, ReportMapping reportMapping, String summary) {
        String field = reportMapping.reportField() == null || reportMapping.reportField().isBlank()
                ? "UNMAPPED"
                : reportMapping.reportField();
        reportDraft.computeIfAbsent(field, key -> new ArrayList<>()).add(summary);
    }

    private record SectionDataContext(Map<RequirementKey, Fact> facts) {

        Fact factFor(DataRequirement requirement) {
            Fact fact = facts.get(RequirementKey.from(requirement));
            if (fact == null) {
                throw new IllegalArgumentException("No preloaded fact for requirement: " + requirement.requirementCode());
            }
            if (fact.samples().size() <= Math.max(requirement.sampleLimit(), 0)) {
                return fact;
            }
            return new Fact(
                    fact.factCode(),
                    fact.domain(),
                    fact.metrics(),
                    fact.samples().stream().limit(Math.max(requirement.sampleLimit(), 0)).toList(),
                    fact.dataQuality()
            );
        }
    }

    private record RequirementKey(
            String domain,
            RequirementScope scope,
            DataGrain grain,
            Map<String, Object> filters,
            List<String> fields
    ) {

        static RequirementKey from(DataRequirement requirement) {
            return new RequirementKey(
                    requirement.domain(),
                    requirement.scope(),
                    requirement.grain(),
                    requirement.filters() == null ? Map.of() : new LinkedHashMap<>(requirement.filters()),
                    requirement.fields() == null ? List.of() : List.copyOf(requirement.fields())
            );
        }
    }
}
