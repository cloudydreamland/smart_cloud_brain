package com.smartcloudbrain.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiOrchestrationServiceTest {

  @Test
  void orchestratesAllFourPrimaryProviderTasks() {
    AiProvider provider = mock(AiProvider.class);
    AiTaskLogService logService = mock(AiTaskLogService.class);
    PromptTemplateService promptService = mock(PromptTemplateService.class);
    PromptResolveResponse prompt = new PromptResolveResponse("GENERAL", "GENERAL", "template", "prompt", "{}", "v1");
    when(promptService.resolve(any(), any())).thenReturn(prompt);
    when(provider.triage(any(), any())).thenReturn(new TriageResponse(
        "心内科", "CARDIOLOGY", List.of(1L), "胸痛", false));
    when(provider.generateMedicalRecord(any(), any())).thenReturn(new MedicalRecordGenerateResponse(
        "胸痛", "两天", "无", "正常", "待查", "复查", false));
    when(provider.checkPrescription(any(), any())).thenReturn(new PrescriptionCheckResponse(
        "LOW", "可用", List.of(), false));
    when(provider.suggestSchedule(any(), any())).thenReturn(new ScheduleSuggestResponse(
        List.of(new ScheduleSuggestionItem(1L, 1L, LocalDate.of(2026, 6, 21), "09:00-12:00", 12, "需求")),
        "dify", false));
    AiOrchestrationService service = new AiOrchestrationService(provider, logService, promptService);

    assertEquals("CARDIOLOGY", service.triage(new TriageRequest(1L, "胸痛")).departmentCode());
    assertEquals("待查", service.generateMedicalRecord(
        new MedicalRecordGenerateRequest(1L, null, "胸痛")).diagnosis());
    assertEquals("LOW", service.checkPrescription(
        new PrescriptionCheckRequest(1L, 1L, List.of())).riskLevel());
    assertEquals(1, service.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1,
        List.of(new ScheduleDoctorCandidate(1L, "医生", 1L, "CARDIOLOGY", "胸痛", true)),
        List.of(), List.of())).suggestions().size());
  }

  @Test
  void fallsBackWhenDifyAndFailureLoggingBothFail() {
    AiProvider provider = mock(AiProvider.class);
    AiTaskLogService logService = mock(AiTaskLogService.class);
    PromptTemplateService promptService = mock(PromptTemplateService.class);
    RuntimeException original = new IllegalStateException("Dify gateway returned 502");
    when(promptService.resolve("TRIAGE", "GENERAL")).thenThrow(original);
    when(provider.providerName()).thenReturn("dify");
    when(provider.modelName()).thenReturn("workflow");
    doThrow(new IllegalStateException("status must not be null"))
        .when(logService).record(any(), any(), any(), any(), any(), any(), any(), any(Long.class), any(Boolean.class), any());

    AiOrchestrationService service = new AiOrchestrationService(provider, logService, promptService);

    var response = service.triage(new TriageRequest(1L, "chest pain"));

    assertEquals("CARDIOLOGY", response.departmentCode());
    assertTrue(response.degraded());
    assertTrue(original.getSuppressed().length >= 1);
  }

  @Test
  void fallsBackForMedicalRecordPrescriptionAndSchedule() {
    AiProvider provider = mock(AiProvider.class);
    AiTaskLogService logService = mock(AiTaskLogService.class);
    PromptTemplateService promptService = mock(PromptTemplateService.class);
    when(provider.providerName()).thenReturn("dify");
    when(promptService.resolve(any(), any())).thenThrow(new IllegalStateException("Dify unavailable"));
    AiOrchestrationService service = new AiOrchestrationService(provider, logService, promptService);

    assertTrue(service.generateMedicalRecord(new MedicalRecordGenerateRequest(1L, "CARDIOLOGY", "胸痛")).degraded());
    assertTrue(service.checkPrescription(new PrescriptionCheckRequest(1L, 1L, List.of())).degraded());
    assertTrue(service.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1,
        List.of(new ScheduleDoctorCandidate(1L, "医生", 1L, "CARDIOLOGY", "胸痛", true)),
        List.of(), List.of())).degraded());
  }

  @Test
  void reportsWhenPrimaryAndFallbackBothFailAndUnwrapsRootCause() {
    AiProvider provider = mock(AiProvider.class);
    AiTaskLogService logService = mock(AiTaskLogService.class);
    PromptTemplateService promptService = mock(PromptTemplateService.class);
    when(provider.providerName()).thenReturn("dify");
    when(promptService.resolve("TRIAGE", "GENERAL"))
        .thenThrow(new IllegalStateException("outer", new IllegalArgumentException("root")));
    AiOrchestrationService service = new AiOrchestrationService(provider, logService, promptService);

    assertThrows(com.smartcloudbrain.common.exception.BusinessException.class,
        () -> service.triage(new TriageRequest(1L, null)));
  }
}
