package com.smartcloudbrain.diagnosis.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeEntrySaveRequest(
    Long id,
    @NotBlank String title,
    @NotBlank String symptoms,
    String riskSignals,
    @NotBlank String advice,
    String departmentCode,
    String status
) {
}
