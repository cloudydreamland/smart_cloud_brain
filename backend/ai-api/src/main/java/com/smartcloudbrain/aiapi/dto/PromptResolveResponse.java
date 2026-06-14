package com.smartcloudbrain.aiapi.dto;

public record PromptResolveResponse(
    String taskType,
    String departmentCode,
    String templateName,
    String templateContent,
    String outputSchema,
    String version
) {
}
