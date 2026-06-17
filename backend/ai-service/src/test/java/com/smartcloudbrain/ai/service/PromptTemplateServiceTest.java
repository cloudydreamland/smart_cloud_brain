package com.smartcloudbrain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.ai.entity.PromptTemplate;
import com.smartcloudbrain.ai.repository.PromptTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {

  @Test
  void resolvesCardiologyMedicalRecordPrompt() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("MEDICAL_RECORD", "CARDIOLOGY", true))
        .thenReturn(List.of(template("MEDICAL_RECORD", "CARDIOLOGY", "CARDIOLOGY_MEDICAL_RECORD_v1", "focus on chest pain")));
    PromptTemplateService service = new PromptTemplateService(repository);

    var response = service.resolve("MEDICAL_RECORD", "CARDIOLOGY");

    assertEquals("CARDIOLOGY_MEDICAL_RECORD_v1", response.templateName());
    assertTrue(response.templateContent().contains("chest pain"));
  }

  @Test
  void defaultsDepartmentWhenMissing() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1", "Recommend department")));
    PromptTemplateService service = new PromptTemplateService(repository);

    var response = service.resolve("TRIAGE", "");

    assertEquals("GENERAL", response.departmentCode());
    assertTrue(response.templateContent().contains("Recommend department"));
  }

  private PromptTemplate template(String taskType, String departmentCode, String name, String content) {
    PromptTemplate template = new PromptTemplate();
    template.setTaskType(taskType);
    template.setDepartmentCode(departmentCode);
    template.setTemplateName(name);
    template.setTemplateContent(content);
    template.setOutputSchema("{}");
    template.setVersion("v1");
    template.setEnabled(true);
    return template;
  }
}
