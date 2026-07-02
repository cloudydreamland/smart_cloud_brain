package com.smartcloudbrain.triage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.triage.dto.internal.InternalTriageAssignRequest;
import com.smartcloudbrain.triage.dto.internal.InternalTriageCloseRequest;
import com.smartcloudbrain.triage.service.TriageDeskService;
import java.util.List;
import java.util.Map;
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

  @Test
  void detail_validatesInternalTokenAndReturnsResult() {
    Map<String, Object> expected = Map.of("triageRecordId", 1L, "status", "WAITING");
    when(triageDeskService.detail(1L)).thenReturn(expected);

    Result<?> result = controller.detail(1L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(internalRequestGuard).requireServiceRequest();
    verify(triageDeskService).detail(1L);
  }

  @Test
  void detail_rejectsWhenInternalTokenIsMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class, () -> controller.detail(1L));

    verify(triageDeskService, never()).detail(1L);
  }

  @Test
  void close_validatesInternalTokenAndReturnsResult() {
    Map<String, Object> expected = Map.of("triageRecordId", 1L, "status", "CLOSED");
    when(triageDeskService.close(1L)).thenReturn(expected);

    Result<?> result = controller.close(new InternalTriageCloseRequest(1L));

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(internalRequestGuard).requireServiceRequest();
    verify(triageDeskService).close(1L);
  }

  @Test
  void close_rejectsWhenInternalTokenIsMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED)).when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class, () -> controller.close(new InternalTriageCloseRequest(1L)));

    verify(triageDeskService, never()).close(1L);
  }

  @Test
  void list_returnsSuccessWithData() {
    List<Map<String, Object>> expected = List.of(
        Map.of("triageRecordId", 1L, "status", "WAITING"),
        Map.of("triageRecordId", 2L, "status", "ASSIGNED")
    );
    when(triageDeskService.list()).thenReturn(expected);

    Result<?> result = controller.list();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(internalRequestGuard).requireServiceRequest();
  }

  @Test
  void assign_validatesInternalTokenAndDelegates() {
    Map<String, Object> expected = Map.of("triageRecordId", 1L, "status", "ASSIGNED", "assignedDoctorId", 2L);
    when(triageDeskService.assign(1L, 2L)).thenReturn(expected);

    Result<?> result = controller.assign(new InternalTriageAssignRequest(1L, 2L));

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(internalRequestGuard).requireServiceRequest();
    verify(triageDeskService).assign(1L, 2L);
  }
}
