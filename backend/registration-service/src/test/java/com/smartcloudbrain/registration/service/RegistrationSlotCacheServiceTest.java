package com.smartcloudbrain.registration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class RegistrationSlotCacheServiceTest {

  private final CacheManager cacheManager = mock(CacheManager.class);
  private final RegistrationSlotCacheService service = new RegistrationSlotCacheService(cacheManager);

  @Test
  void evictsSlotsCache() {
    Cache cache = mock(Cache.class);
    when(cacheManager.getCache("registration:slots")).thenReturn(cache);

    service.evictSlotsCache();

    verify(cache).clear();
  }

  @Test
  void cacheFailureDoesNotBreakEvictRequest() {
    Cache cache = mock(Cache.class);
    when(cacheManager.getCache("registration:slots")).thenReturn(cache);
    doThrow(new RuntimeException("redis down")).when(cache).clear();

    assertDoesNotThrow(service::evictSlotsCache);
  }
}
