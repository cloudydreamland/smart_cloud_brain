package com.smartcloudbrain.ai.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.smartcloudbrain.ai.application.AiOrchestrationService;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InternalAiControllerSecurityTest {

  private final AiOrchestrationService aiOrchestrationService = mock(AiOrchestrationService.class);
  private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
  private final InternalRequestGuard internalRequestGuard = mock(InternalRequestGuard.class);
  private final InternalAiController controller =
      new InternalAiController(aiOrchestrationService, promptTemplateService, internalRequestGuard);

  @Test
  void validatesInternalTokenForAiEndpoints() {
    controller.triage(new TriageRequest(1L, "fever", "", null, null, null, null));
    controller.resolvePrompt(new PromptResolveRequest("TRIAGE", "GENERAL"));
    controller.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1, List.of(), List.of(), List.of()));

    verify(internalRequestGuard, times(3)).requireServiceRequest();
  }

  @Test
  void rejectsAiEndpointWhenInternalTokenIsMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class,
        () -> controller.triage(new TriageRequest(1L, "fever", "", null, null, null, null)));

    verify(aiOrchestrationService, never()).triage(any());
  }
}
