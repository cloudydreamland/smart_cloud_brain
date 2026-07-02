package com.smartcloudbrain.patient.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.patient.service.PatientCatalogCacheService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalPatientCacheControllerTest {

  private final PatientCatalogCacheService cacheService = mock(PatientCatalogCacheService.class);
  private final InternalRequestGuard internalRequestGuard = mock(InternalRequestGuard.class);
  private final InternalPatientCacheController controller =
      new InternalPatientCacheController(cacheService, internalRequestGuard);

  @Test
  void evictCatalogCachesValidatesInternalTokenAndReturnsEvictedFlag() {
    var result = controller.evictCatalogCaches();

    verify(internalRequestGuard).requireServiceRequest();
    verify(cacheService).evictCatalogCaches();
    assertEquals(true, ((Map<?, ?>) result.data()).get("evicted"));
  }

  @Test
  void evictCatalogCachesRejectsWhenInternalTokenMissing() {
    doThrow(new BusinessException(ErrorCode.UNAUTHORIZED))
        .when(internalRequestGuard).requireServiceRequest();

    assertThrows(BusinessException.class, () -> controller.evictCatalogCaches());
    verify(cacheService, never()).evictCatalogCaches();
  }
}
