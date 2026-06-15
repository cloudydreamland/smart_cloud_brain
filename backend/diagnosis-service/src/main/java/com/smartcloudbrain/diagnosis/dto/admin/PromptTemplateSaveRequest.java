package com.smartcloudbrain.diagnosis.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record PromptTemplateSaveRequest(
    Long id,
    @NotBlank String taskType,
    String departmentCode,
    @NotBlank String templateName,
    @NotBlank String templateContent,
    String outputSchema,
    String version,
    Boolean enabled
) {
}
