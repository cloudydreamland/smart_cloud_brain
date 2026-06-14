package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptResolveRequest(
    @NotBlank String taskType,
    String departmentCode
) {
}
