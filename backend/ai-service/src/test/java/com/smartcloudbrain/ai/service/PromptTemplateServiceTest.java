package com.smartcloudbrain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {

  private final PromptTemplateService service = new PromptTemplateService();

  @Test
  void resolvesCardiologyMedicalRecordPrompt() {
    var response = service.resolve("MEDICAL_RECORD", "CARDIOLOGY");

    assertEquals("CARDIOLOGY_MEDICAL_RECORD_v1", response.templateName());
    assertTrue(response.templateContent().contains("chest pain"));
  }

  @Test
  void defaultsDepartmentWhenMissing() {
    var response = service.resolve("TRIAGE", "");

    assertEquals("GENERAL", response.departmentCode());
    assertTrue(response.templateContent().contains("Recommend department"));
  }
}
