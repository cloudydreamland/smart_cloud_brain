package com.smartcloudbrain.ai.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.ai.application.AiOrchestrationService;
import com.smartcloudbrain.ai.application.AiTaskLogService;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.PromptTestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class InternalAiControllerTest {

  private final AiOrchestrationService aiOrchestrationService = mock(AiOrchestrationService.class);
  private final AiTaskLogService aiTaskLogService = mock(AiTaskLogService.class);
  private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
  private final InternalRequestGuard internalRequestGuard = mock(InternalRequestGuard.class);
  private final RedisRateLimiter redisRateLimiter = mock(RedisRateLimiter.class);
  private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private final InternalAiController controller =
      new InternalAiController(aiOrchestrationService, aiTaskLogService, promptTemplateService,
          internalRequestGuard, redisRateLimiter, httpServletRequest);

  @BeforeEach
  void setUp() {
    lenient().when(redisRateLimiter.allow(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
    lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  // ── triage ──────────────────────────────────────────────────────

  @Test
  void triageDelegatesToOrchestrationService() {
    TriageRequest request = new TriageRequest(1L, "fever");
    TriageResponse response = new TriageResponse("内科", "GENERAL", List.of(1L), "fever", false);
    when(aiOrchestrationService.triage(request)).thenReturn(response);

    Result<?> result = controller.triage(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
    verify(internalRequestGuard).requireServiceRequest();
    verify(aiOrchestrationService).triage(request);
  }

  // ── generateMedicalRecord ───────────────────────────────────────

  @Test
  void generateMedicalRecordDelegatesToService() {
    MedicalRecordGenerateRequest request = new MedicalRecordGenerateRequest(1L, "GENERAL", "patient has fever");
    MedicalRecordGenerateResponse response = new MedicalRecordGenerateResponse(
        "fever", "patient has fever", "", "", "感冒", "多喝水", false);
    when(aiOrchestrationService.generateMedicalRecord(request)).thenReturn(response);

    Result<?> result = controller.generateMedicalRecord(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  // ── streamMedicalRecord ─────────────────────────────────────────

  @Test
  void streamMedicalRecordReturnsEmitter() {
    MedicalRecordGenerateResponse response = new MedicalRecordGenerateResponse(
        "fever", "patient text", "", "", "感冒", "rest", false);
    when(aiOrchestrationService.generateMedicalRecord(any(MedicalRecordGenerateRequest.class)))
        .thenReturn(response);

    SseEmitter emitter = controller.streamMedicalRecord(1L, "patient text", "GENERAL");

    assertNotNull(emitter);
    verify(internalRequestGuard).requireServiceRequest();
    // Give the background thread a moment to complete
    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
  }

  // ── checkPrescription ───────────────────────────────────────────

  @Test
  void checkPrescriptionDelegatesToService() {
    List<DrugItem> drugs = List.of(new DrugItem("Aspirin", "100mg", "once daily", "oral", 7, ""));
    PrescriptionCheckRequest request = new PrescriptionCheckRequest(1L, 2L, drugs);
    PrescriptionCheckResponse response = new PrescriptionCheckResponse(
        "LOW", "no risks", "ok", List.of(), List.of(), List.of(), false);
    when(aiOrchestrationService.checkPrescription(request)).thenReturn(response);

    Result<?> result = controller.checkPrescription(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  // ── suggestSchedule ─────────────────────────────────────────────

  @Test
  void suggestScheduleDelegatesToService() {
    ScheduleSuggestRequest request = new ScheduleSuggestRequest(
        LocalDate.of(2026, 7, 1), 1, List.of(), List.of(), List.of());
    ScheduleSuggestResponse response = new ScheduleSuggestResponse(List.of(), "mock", false);
    when(aiOrchestrationService.suggestSchedule(request)).thenReturn(response);

    Result<?> result = controller.suggestSchedule(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  // ── resolvePrompt ───────────────────────────────────────────────

  @Test
  void resolvePromptDelegatesToTemplateService() {
    PromptResolveRequest request = new PromptResolveRequest("TRIAGE", "GENERAL");
    PromptResolveResponse response = new PromptResolveResponse(1L, "TRIAGE", "GENERAL",
        "template", "content", "{}", "1.0");
    when(promptTemplateService.resolve("TRIAGE", "GENERAL")).thenReturn(response);

    Result<?> result = controller.resolvePrompt(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  // ── testPrompt ──────────────────────────────────────────────────

  @Test
  void testPromptDelegatesToService() {
    PromptTestRequest request = new PromptTestRequest("TRIAGE", "GENERAL", "tpl",
        "content", "{}", "sample");
    TriageResponse response = new TriageResponse("内科", "GENERAL", List.of(1L), "ok", false);
    when(aiOrchestrationService.testPrompt(request)).thenReturn(response);

    Result<?> result = controller.testPrompt(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  // ── recentLogs ──────────────────────────────────────────────────

  @Test
  void recentLogsDelegatesToLogService() {
    List<Map<String, Object>> logs = List.of(Map.of("taskType", "TRIAGE"));
    when(aiTaskLogService.recentLogs()).thenReturn(logs);

    Result<?> result = controller.recentLogs();

    assertEquals(0, result.code());
    assertEquals(logs, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  // ── rate limiting ───────────────────────────────────────────────

  @Test
  void throws429WhenRateLimitExceeded() {
    when(redisRateLimiter.allow(anyString(), eq(20), any(Duration.class))).thenReturn(false);

    assertThrows(BusinessException.class,
        () -> controller.triage(new TriageRequest(1L, "fever")));
  }

  @Test
  void rateLimitUsesCallerHeaderWhenPresent() {
    when(httpServletRequest.getHeader("X-Internal-Caller")).thenReturn("gateway");

    controller.triage(new TriageRequest(1L, "fever"));

    verify(redisRateLimiter).allow("rate:ai:internal:gateway", 20, Duration.ofMinutes(1));
  }

  @Test
  void rateLimitFallsBackToRemoteAddrWhenCallerHeaderBlank() {
    when(httpServletRequest.getHeader("X-Internal-Caller")).thenReturn("  ");

    controller.triage(new TriageRequest(1L, "fever"));

    verify(redisRateLimiter).allow("rate:ai:internal:127.0.0.1", 20, Duration.ofMinutes(1));
  }

  @Test
  void rateLimitUsesUnknownWhenBothCallerAndAddrAreNull() {
    when(httpServletRequest.getHeader("X-Internal-Caller")).thenReturn(null);
    when(httpServletRequest.getRemoteAddr()).thenReturn(null);

    controller.triage(new TriageRequest(1L, "fever"));

    verify(redisRateLimiter).allow("rate:ai:internal:unknown", 20, Duration.ofMinutes(1));
  }

  // ── resolvePrompt does NOT enforce rate limit ───────────────────

  @Test
  void resolvePromptDoesNotEnforceRateLimit() {
    PromptResolveRequest request = new PromptResolveRequest("TRIAGE", "GENERAL");
    when(promptTemplateService.resolve("TRIAGE", "GENERAL")).thenReturn(
        new PromptResolveResponse(1L, "TRIAGE", "GENERAL", "tpl", "content", "{}", "1.0"));

    controller.resolvePrompt(request);

    verify(redisRateLimiter, never()).allow(anyString(), anyInt(), any(Duration.class));
  }
}
