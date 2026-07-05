package com.easylink.ruleproduct.core.model;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ExecutionResult(
        String executionId,
        String tenantId,
        String dataSourceProfile,
        String indicatorCode,
        String clientId,
        LocalDate alertDate,
        LocalDate warningStartDate,
        LocalDate warningEndDate,
        LocalDateTime executedAt,
        List<StepExecutionResult> steps,
        Map<String, List<String>> reportDraft
) {
}
