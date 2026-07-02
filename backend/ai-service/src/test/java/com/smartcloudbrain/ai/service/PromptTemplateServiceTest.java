package com.smartcloudbrain.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.ai.entity.Department;
import com.smartcloudbrain.ai.entity.PromptTemplate;
import com.smartcloudbrain.ai.repository.DepartmentRepository;
import com.smartcloudbrain.ai.repository.PromptTemplateRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {

  // ── Direct match: specific department ───────────────────────────────────

  @Test
  void resolvesCardiologyMedicalRecordPrompt() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("MEDICAL_RECORD", "CARDIOLOGY", true))
        .thenReturn(List.of(template("MEDICAL_RECORD", "CARDIOLOGY", "CARDIOLOGY_MEDICAL_RECORD_v1", "focus on chest pain")));
    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("MEDICAL_RECORD", "CARDIOLOGY");

    assertEquals("CARDIOLOGY_MEDICAL_RECORD_v1", response.templateName());
    assertTrue(response.templateContent().contains("chest pain"));
  }

  // ── Default department when blank ──────────────────────────────────────

  @Test
  void defaultsDepartmentWhenMissing() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1", "Recommend department")));
    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("TRIAGE", "");

    assertEquals("GENERAL", response.departmentCode());
    assertTrue(response.templateContent().contains("Recommend department"));
  }

  // ── Null department defaults to GENERAL ────────────────────────────────

  @Test
  void nullDepartmentDefaultsToGeneral() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1", "General triage")));
    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("TRIAGE", null);

    assertEquals("GENERAL", response.departmentCode());
  }

  // ── Department-specific not found → fallback to GENERAL ────────────────

  @Test
  void fallsBackToGeneralWhenDepartmentSpecificNotFound() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    // CARDIOLOGY-specific template not found
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "CARDIOLOGY", true))
        .thenReturn(List.of());
    // Fallback to GENERAL
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1", "General fallback")));
    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = new PromptTemplateService(repository, departmentRepository).resolve("TRIAGE", "CARDIOLOGY");

    assertEquals("GENERAL", response.departmentCode());
    assertTrue(response.templateContent().contains("General fallback"));
  }

  // ── No template found at all → BusinessException ───────────────────────

  @Test
  void throwsWhenNoTemplateFoundForAnyDepartment() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("UNKNOWN_TASK", "GENERAL", true))
        .thenReturn(List.of());
    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.resolve("UNKNOWN_TASK", "GENERAL"));
    assertTrue(ex.getMessage().contains("UNKNOWN_TASK"));
  }

  // ── No template for specific dept, and no GENERAL fallback either ──────

  @Test
  void throwsWhenNoTemplateForSpecificDeptNorGeneral() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "CARDIOLOGY", true))
        .thenReturn(List.of());
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of());
    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    assertThrows(BusinessException.class,
        () -> service.resolve("TRIAGE", "CARDIOLOGY"));
  }

  // ── Template with {{departmentList}} placeholder ────────────────────────

  @Test
  void resolvesDepartmentListPlaceholder() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);

    PromptTemplate tpl = template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1",
        "Available departments: {{departmentList}}");
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(tpl));

    Department d1 = new Department();
    d1.setCode("CARDIOLOGY");
    d1.setName("心内科");
    Department d2 = new Department();
    d2.setCode("GENERAL");
    d2.setName("全科门诊");
    when(departmentRepository.findAllByOrderByIdAsc()).thenReturn(List.of(d1, d2));

    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("TRIAGE", "GENERAL");

    assertTrue(response.templateContent().contains("心内科(CARDIOLOGY)"));
    assertTrue(response.templateContent().contains("全科门诊(GENERAL)"));
    assertTrue(response.templateContent().contains("、"));
  }

  // ── Template without placeholder → no department list substitution ──────

  @Test
  void templateWithoutPlaceholderSkipsDepartmentListResolution() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);

    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1", "Simple prompt")));

    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("TRIAGE", "GENERAL");

    assertEquals("Simple prompt", response.templateContent());
  }

  // ── Response fields populated correctly from template ──────────────────

  @Test
  void responseFieldsMatchTemplateProperties() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);

    PromptTemplate tpl = new PromptTemplate();
    tpl.setId(42L);
    tpl.setTaskType("PRESCRIPTION_CHECK");
    tpl.setDepartmentCode("GENERAL");
    tpl.setTemplateName("RX_CHECK_v2");
    tpl.setTemplateContent("Check drugs");
    tpl.setOutputSchema("{\"type\":\"object\"}");
    tpl.setVersion("2.0");
    tpl.setEnabled(true);

    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("PRESCRIPTION_CHECK", "GENERAL", true))
        .thenReturn(List.of(tpl));

    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("PRESCRIPTION_CHECK", "GENERAL");

    assertEquals(42L, response.promptTemplateId());
    assertEquals("PRESCRIPTION_CHECK", response.taskType());
    assertEquals("GENERAL", response.departmentCode());
    assertEquals("RX_CHECK_v2", response.templateName());
    assertEquals("Check drugs", response.templateContent());
    assertEquals("{\"type\":\"object\"}", response.outputSchema());
    assertEquals("2.0", response.version());
  }

  // ── Template with null content ──────────────────────────────────────────

  @Test
  void templateWithNullContentHandledGracefully() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);

    PromptTemplate tpl = template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1", null);
    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(tpl));

    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("TRIAGE", "GENERAL");
    assertNotNull(response);
  }

  // ── Template with empty departmentList placeholder and empty depts ──────

  @Test
  void departmentListPlaceholderWithNoDepartments() {
    PromptTemplateRepository repository = mock(PromptTemplateRepository.class);
    DepartmentRepository departmentRepository = mock(DepartmentRepository.class);

    when(repository.findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc("TRIAGE", "GENERAL", true))
        .thenReturn(List.of(template("TRIAGE", "GENERAL", "GENERAL_TRIAGE_v1",
            "Departments: {{departmentList}}")));
    when(departmentRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

    PromptTemplateService service = new PromptTemplateService(repository, departmentRepository);

    var response = service.resolve("TRIAGE", "GENERAL");

    assertTrue(response.templateContent().contains("Departments: "));
    // no departments → empty string after replacement
    assertEquals("Departments: ", response.templateContent());
  }

  // ── Helper ──────────────────────────────────────────────────────────────

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
