package com.smartcloudbrain.patient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PatientCatalogCacheService {

  static final String DEPARTMENT_CACHE = "patient:department:list";
  static final String DOCTOR_CACHE = "patient:doctor:list";

  private static final Logger log = LoggerFactory.getLogger(PatientCatalogCacheService.class);

  private final CacheManager cacheManager;

  public PatientCatalogCacheService(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public void evictCatalogCaches() {
    evict(DEPARTMENT_CACHE);
    evict(DOCTOR_CACHE);
  }

  private void evict(String cacheName) {
    try {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache != null) {
        cache.clear();
      }
    } catch (RuntimeException exception) {
      log.warn("Patient catalog cache evict failed for cache={}: {}", cacheName, exception.getMessage());
    }
  }
}
