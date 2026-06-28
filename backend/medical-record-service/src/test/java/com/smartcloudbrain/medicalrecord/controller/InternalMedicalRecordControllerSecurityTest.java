package com.smartcloudbrain.medicalrecord.controller;

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
import com.smartcloudbrain.medicalrecord.service.MedicalRecordService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalMedicalRecordControllerSecurityTest {

  private final MedicalRecordService medicalRecordService = mock(MedicalRecordService.class);
  private final InternalRequestGuard internalRequestGuard = mock(InternalRequestGuard.class);
  private final InternalMedicalRecordController controller =
      new InternalMedicalRecordController(medicalRecordService, internalRequestGuard);

  @Test
  void validatesInternalTokenAndReturnsPatientRecords() {
    when(medicalRecordService.recordsByPatient(1L)).thenReturn(List.of(Map.of("patientId", 1L)));

    assertNotNull(controller.byPatient(1L).data());

    verify(internalRequestGuard).requireServiceRequest();
    verify(medicalRecordService).recordsByPatient(1L);
  }

  @Test
  void rejectsPatientRecordsWhenInternalTokenIsMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class, () -> controller.byPatient(1L));

    verify(medicalRecordService, never()).recordsByPatient(1L);
  }
}
