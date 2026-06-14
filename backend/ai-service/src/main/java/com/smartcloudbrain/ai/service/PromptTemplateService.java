package com.smartcloudbrain.ai.service;

import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptTemplateService {

  public PromptResolveResponse resolve(String taskType, String departmentCode) {
    String normalizedDepartment = StringUtils.hasText(departmentCode) ? departmentCode : "GENERAL";
    String templateName = normalizedDepartment + "_" + taskType + "_v1";
    String templateContent = switch (taskType) {
      case "MEDICAL_RECORD" -> "Generate a structured medical record JSON. Include chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis and treatmentAdvice.";
      case "PRESCRIPTION_CHECK" -> "Check prescription safety and return riskLevel, suggestions and interactions as JSON.";
      case "TRIAGE" -> "Recommend department and doctors from the chief complaint. Return departmentCode, recommendedDepartment and reason as JSON.";
      default -> "Return a valid JSON result for the requested AI task.";
    };
    if ("CARDIOLOGY".equalsIgnoreCase(normalizedDepartment) && "MEDICAL_RECORD".equals(taskType)) {
      templateContent = templateContent + " Focus on chest pain, dyspnea, ECG and cardiovascular risk factors.";
    }
    String outputSchema = "{\"type\":\"object\",\"required\":[\"result\"]}";
    return new PromptResolveResponse(taskType, normalizedDepartment, templateName, templateContent, outputSchema, "v1");
  }
}
