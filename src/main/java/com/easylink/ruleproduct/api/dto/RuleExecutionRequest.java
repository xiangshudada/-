package com.easylink.ruleproduct.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RuleExecutionRequest(
        String tenantId,
        String dataSourceProfile,
        @NotBlank String indicatorCode,
        @NotBlank String clientId,
        @NotNull LocalDate alertDate,
        LocalDate warningStartDate,
        LocalDate warningEndDate
) {
    public String tenantIdOrDefault() {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    public String dataSourceProfileOrDefault() {
        return dataSourceProfile == null || dataSourceProfile.isBlank() ? "default" : dataSourceProfile;
    }
}
