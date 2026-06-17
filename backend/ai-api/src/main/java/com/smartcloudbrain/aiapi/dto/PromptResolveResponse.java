package com.smartcloudbrain.aiapi.dto;

public record PromptResolveResponse(
    Long promptTemplateId,
    String taskType,
    String departmentCode,
    String templateName,
    String templateContent,
    String outputSchema,
    String version
) {
  public PromptResolveResponse(
      String taskType,
      String departmentCode,
      String templateName,
      String templateContent,
      String outputSchema,
      String version
  ) {
    this(null, taskType, departmentCode, templateName, templateContent, outputSchema, version);
  }
}
