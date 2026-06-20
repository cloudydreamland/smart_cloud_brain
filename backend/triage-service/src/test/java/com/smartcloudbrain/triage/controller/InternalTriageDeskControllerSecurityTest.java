package com.smartcloudbrain.triage.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.triage.dto.internal.InternalTriageAssignRequest;
import com.smartcloudbrain.triage.service.TriageDeskService;
import org.junit.jupiter.api.Test;

class InternalTriageDeskControllerSecurityTest {

  private final TriageDeskService triageDeskService = mock(TriageDeskService.class);
  private final InternalRequestGuard internalRequestGuard = mock(InternalRequestGuard.class);
  private final InternalTriageDeskController controller =
      new InternalTriageDeskController(triageDeskService, internalRequestGuard);

  @Test
  void validatesInternalTokenForTriageDeskEndpoints() {
    controller.list();
    controller.assign(new InternalTriageAssignRequest(1L, 2L));

    verify(internalRequestGuard, times(2)).requireServiceRequest();
  }

  @Test
  void rejectsTriageDeskEndpointWhenInternalTokenIsMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class, controller::list);

    verify(triageDeskService, never()).list();
  }
}
