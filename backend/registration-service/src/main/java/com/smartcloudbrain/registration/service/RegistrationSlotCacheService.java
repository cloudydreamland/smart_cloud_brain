package com.smartcloudbrain.registration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class RegistrationSlotCacheService {

  static final String SLOTS_CACHE = "registration:slots";

  private static final Logger log = LoggerFactory.getLogger(RegistrationSlotCacheService.class);

  private final CacheManager cacheManager;

  public RegistrationSlotCacheService(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public void evictSlotsCache() {
    try {
      Cache cache = cacheManager.getCache(SLOTS_CACHE);
      if (cache != null) {
        cache.clear();
      }
    } catch (RuntimeException exception) {
      log.warn("Registration slots cache evict failed for cache={}: {}", SLOTS_CACHE, exception.getMessage());
    }
  }
}
