package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptTestRequest(
    @NotBlank String taskType,
    String departmentCode,
    String templateName,
    @NotBlank String templateContent,
    String outputSchema,
    String sampleInput
) {
}
