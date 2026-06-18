package com.smartcloudbrain.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AiOrchestrationServiceTest {

  @Test
  void preservesBusinessFailureWhenFailureLogCannotBePersisted() {
    AiProvider provider = mock(AiProvider.class);
    AiTaskLogService logService = mock(AiTaskLogService.class);
    PromptTemplateService promptService = mock(PromptTemplateService.class);
    BusinessException original = new BusinessException(600, "Dify gateway returned 502");
    when(promptService.resolve("TRIAGE", "GENERAL")).thenThrow(original);
    when(provider.providerName()).thenReturn("dify");
    when(provider.modelName()).thenReturn("workflow");
    doThrow(new IllegalStateException("status must not be null"))
        .when(logService).record(any(), any(), any(), any(), any(), any(), any(), any(Long.class), any(Boolean.class), any());

    AiOrchestrationService service = new AiOrchestrationService(provider, logService, promptService);

    BusinessException thrown = assertThrows(BusinessException.class, () -> service.triage(mock(TriageRequest.class)));
    assertSame(original, thrown);
    assertEquals(1, thrown.getSuppressed().length);
    assertEquals("status must not be null", thrown.getSuppressed()[0].getMessage());
  }
}
