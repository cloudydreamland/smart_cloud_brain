package com.smartcloudbrain.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.entity.AiGenerationLog;
import com.smartcloudbrain.ai.repository.AiGenerationLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiTaskLogServiceTest {

  private final AiGenerationLogRepository repository = mock(AiGenerationLogRepository.class);
  private final AiTaskLogService service = new AiTaskLogService(repository, new ObjectMapper());

  @Test
  void recordsSuccessStatusForSuccessfulGeneration() {
    service.record(
        "MEDICAL_RECORD",
        "mock",
        "mock-model",
        "request-1",
        "input",
        "output",
        null,
        12L,
        true,
        ""
    );

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals("SUCCESS", captor.getValue().getStatus());
  }

  @Test
  void recordsFailedStatusForFailedGeneration() {
    service.record(
        "MEDICAL_RECORD",
        "mock",
        "mock-model",
        "request-2",
        "input",
        null,
        null,
        12L,
        false,
        "provider failed"
    );

    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).saveAndFlush(captor.capture());
    assertEquals("FAILED", captor.getValue().getStatus());
  }

  @Test
  void doesNotThrowWhenLogPersistenceFails() {
    when(repository.saveAndFlush(any(AiGenerationLog.class))).thenThrow(new RuntimeException("db down"));

    assertDoesNotThrow(() -> service.record(
        "MEDICAL_RECORD",
        "mock",
        "mock-model",
        "request-3",
        "input",
        "output",
        null,
        12L,
        true,
        ""
    ));
  }
}
