package com.smartcloudbrain.patient.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.patient.service.PatientService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalPatientControllerSecurityTest {

  private final PatientService patientService = mock(PatientService.class);
  private final InternalRequestGuard internalRequestGuard = mock(InternalRequestGuard.class);
  private final InternalPatientController controller = new InternalPatientController(patientService, internalRequestGuard);

  @Test
  void validatesInternalTokenAndReturnsPatientSummary() {
    when(patientService.patientSummary(1L)).thenReturn(Map.of("patientId", 1L));

    assertNotNull(controller.summary(1L).data());

    verify(internalRequestGuard).requireServiceRequest();
    verify(patientService).patientSummary(1L);
  }

  @Test
  void rejectsPatientSummaryWhenInternalTokenIsMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class, () -> controller.summary(1L));

    verify(patientService, never()).patientSummary(1L);
  }
}
