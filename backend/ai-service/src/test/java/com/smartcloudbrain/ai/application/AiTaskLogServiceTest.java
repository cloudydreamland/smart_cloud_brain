package com.smartcloudbrain.ai.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.entity.AiGenerationLog;
import com.smartcloudbrain.ai.repository.AiGenerationLogRepository;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiTaskLogServiceTest {

  private final AiGenerationLogRepository repository = mock(AiGenerationLogRepository.class);
  private final AiTaskLogService service = new AiTaskLogService(repository, new ObjectMapper());

  // ── record ──────────────────────────────────────────────────────

  @Test
  void recordsSuccessStatusRequiredByLegacySchema() {
    service.record("TRIAGE", "dify", "workflow", "request-1", "input", "output", null, 12L, true, "");

    assertSavedStatus("SUCCESS");
  }

  @Test
  void recordsFailedStatusRequiredByLegacySchema() {
    service.record("TRIAGE", "dify", "workflow", "request-2", "input", null, null, 12L, false, "bad gateway");

    assertSavedStatus("FAILED");
  }

  @Test
  void doesNotThrowWhenLogPersistenceFails() {
    when(repository.saveAndFlush(any(AiGenerationLog.class))).thenThrow(new RuntimeException("db down"));

    assertDoesNotThrow(() -> service.record(
        "MEDICAL_RECORD", "mock", "mock-model", "request-3", "input", "output", null, 12L, true, ""));
  }

  @Test
  void recordWithPromptResolveResponseSetsTemplateId() {
    PromptResolveResponse prompt = new PromptResolveResponse(99L, "TRIAGE", "GENERAL",
        "tpl", "content", "{}", "1.0");

    service.record("TRIAGE", "dify", "workflow", "req-4", "input", "output", prompt, 100L, true, "");

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals(99L, captor.getValue().getPromptTemplateId());
  }

  @Test
  void recordWithNullPromptDoesNotSetTemplateId() {
    service.record("TRIAGE", "dify", "workflow", "req-5", "input", "output", null, 50L, true, "");

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals(null, captor.getValue().getPromptTemplateId());
  }

  @Test
  void recordWithNonStringInputSerializesToObject() {
    Map<String, Object> inputObj = Map.of("key", "value", "count", 42);

    service.record("TRIAGE", "dify", "workflow", "req-6", inputObj, "output", null, 10L, true, "");

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertTrue(captor.getValue().getInputSummary().contains("value"));
  }

  @Test
  void recordTruncatesLongSummaryTo500Chars() {
    String longInput = "x".repeat(1000);

    service.record("TRIAGE", "dify", "workflow", "req-7", longInput, "output", null, 10L, true, "");

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertTrue(captor.getValue().getInputSummary().length() <= 500);
  }

  @Test
  void recordWithNullInputProducesEmptySummary() {
    service.record("TRIAGE", "dify", "workflow", "req-8", null, null, null, 10L, false, "error");

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals("", captor.getValue().getInputSummary());
    // output summary is empty for failed records
    assertEquals("", captor.getValue().getOutputSummary());
  }

  @Test
  void recordCollapsesWhitespace() {
    String input = "hello   world\n\t  foo";

    service.record("TRIAGE", "dify", "workflow", "req-9", input, "output", null, 10L, true, "");

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals("hello world foo", captor.getValue().getInputSummary());
  }

  // ── recentLogs ──────────────────────────────────────────────────

  @Test
  void recentLogsReturnsEmptyListWhenNoLogs() {
    when(repository.findTop20ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

    List<Map<String, Object>> result = service.recentLogs();

    assertTrue(result.isEmpty());
  }

  @Test
  void recentLogsMapsAllFields() {
    AiGenerationLog entry = new AiGenerationLog();
    entry.setTaskType("TRIAGE");
    entry.setProvider("dify");
    entry.setModel("workflow");
    entry.setRequestId("req-1");
    entry.setStatus("SUCCESS");
    entry.setSuccess(true);
    entry.setLatencyMs(150L);
    entry.setPromptTemplateId(42L);
    entry.setErrorMessage("");
    entry.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
    when(repository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(entry));

    List<Map<String, Object>> result = service.recentLogs();

    assertEquals(1, result.size());
    Map<String, Object> view = result.get(0);
    assertEquals("TRIAGE", view.get("taskType"));
    assertEquals("dify", view.get("provider"));
    assertEquals("workflow", view.get("model"));
    assertEquals("req-1", view.get("requestId"));
    assertEquals("SUCCESS", view.get("status"));
    assertEquals(true, view.get("success"));
    assertEquals(150L, view.get("latencyMs"));
    assertEquals(42L, view.get("promptTemplateId"));
    assertEquals("", view.get("errorMessage"));
    assertEquals("2026-07-01T10:00", view.get("createdAt"));
  }

  @Test
  void recentLogsHandlesNullFieldsGracefully() {
    AiGenerationLog entry = new AiGenerationLog();
    entry.setTaskType(null);
    entry.setProvider(null);
    entry.setModel(null);
    entry.setRequestId(null);
    entry.setStatus(null);
    entry.setSuccess(null);
    entry.setLatencyMs(null);
    entry.setPromptTemplateId(null);
    entry.setErrorMessage(null);
    entry.setCreatedAt(null);
    when(repository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(entry));

    List<Map<String, Object>> result = service.recentLogs();

    assertEquals(1, result.size());
    Map<String, Object> view = result.get(0);
    assertEquals("", view.get("taskType"));
    assertEquals("", view.get("provider"));
    assertEquals("", view.get("model"));
    assertEquals("", view.get("requestId"));
    assertEquals("", view.get("status"));
    assertEquals(false, view.get("success"));
    assertEquals(0L, view.get("latencyMs"));
    assertEquals(0L, view.get("promptTemplateId"));
    assertEquals("", view.get("errorMessage"));
    assertEquals("", view.get("createdAt"));
  }

  @Test
  void recentLogsReturnsMultipleEntries() {
    AiGenerationLog e1 = new AiGenerationLog();
    e1.setTaskType("TRIAGE");
    e1.setSuccess(true);
    e1.setStatus("SUCCESS");
    AiGenerationLog e2 = new AiGenerationLog();
    e2.setTaskType("MEDICAL_RECORD");
    e2.setSuccess(false);
    e2.setStatus("FAILED");
    e2.setErrorMessage("timeout");
    when(repository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(e1, e2));

    List<Map<String, Object>> result = service.recentLogs();

    assertEquals(2, result.size());
    assertEquals("TRIAGE", result.get(0).get("taskType"));
    assertEquals("MEDICAL_RECORD", result.get(1).get("taskType"));
    assertFalse((Boolean) result.get(1).get("success"));
  }

  // ── helpers ─────────────────────────────────────────────────────

  private void assertSavedStatus(String expected) {
    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals(expected, captor.getValue().getStatus());
  }
}
