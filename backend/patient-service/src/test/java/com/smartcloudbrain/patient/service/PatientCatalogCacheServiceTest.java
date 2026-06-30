package com.smartcloudbrain.patient.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class PatientCatalogCacheServiceTest {

  private final CacheManager cacheManager = mock(CacheManager.class);
  private final PatientCatalogCacheService service = new PatientCatalogCacheService(cacheManager);

  @Test
  void evictsDepartmentAndDoctorCaches() {
    Cache departmentCache = mock(Cache.class);
    Cache doctorCache = mock(Cache.class);
    when(cacheManager.getCache("patient:department:list")).thenReturn(departmentCache);
    when(cacheManager.getCache("patient:doctor:list")).thenReturn(doctorCache);

    service.evictCatalogCaches();

    verify(departmentCache).clear();
    verify(doctorCache).clear();
  }

  @Test
  void cacheFailureDoesNotBreakEvictRequest() {
    when(cacheManager.getCache("patient:department:list")).thenThrow(new RuntimeException("redis down"));
    Cache doctorCache = mock(Cache.class);
    when(cacheManager.getCache("patient:doctor:list")).thenReturn(doctorCache);
    doThrow(new RuntimeException("redis down")).when(doctorCache).clear();

    assertDoesNotThrow(service::evictCatalogCaches);
  }
}
