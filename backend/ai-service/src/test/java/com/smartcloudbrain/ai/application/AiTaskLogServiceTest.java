package com.smartcloudbrain.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.entity.AiGenerationLog;
import com.smartcloudbrain.ai.repository.AiGenerationLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiTaskLogServiceTest {

  private final AiGenerationLogRepository repository = mock(AiGenerationLogRepository.class);
  private final AiTaskLogService service = new AiTaskLogService(repository, new ObjectMapper());

  @Test
  void recordsSuccessStatusRequiredByLegacySchema() {
    service.record("TRIAGE", "dify", "workflow", "request-1", "input", "output", null, 12, true, "");

    assertSavedStatus("SUCCESS");
  }

  @Test
  void recordsFailedStatusRequiredByLegacySchema() {
    service.record("TRIAGE", "dify", "workflow", "request-2", "input", null, null, 12, false, "bad gateway");

    assertSavedStatus("FAILED");
  }

  private void assertSavedStatus(String expected) {
    ArgumentCaptor<AiGenerationLog> captor = ArgumentCaptor.forClass(AiGenerationLog.class);
    verify(repository).save(captor.capture());
    assertEquals(expected, captor.getValue().getStatus());
  }
}
