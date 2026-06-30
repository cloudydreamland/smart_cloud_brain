package com.smartcloudbrain.patient.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.patient.service.PatientCatalogCacheService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/cache/catalog")
public class InternalPatientCacheController {

  private final PatientCatalogCacheService patientCatalogCacheService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalPatientCacheController(
      PatientCatalogCacheService patientCatalogCacheService,
      InternalRequestGuard internalRequestGuard
  ) {
    this.patientCatalogCacheService = patientCatalogCacheService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @PostMapping("/evict")
  public Result<?> evictCatalogCaches() {
    internalRequestGuard.requireServiceRequest();
    patientCatalogCacheService.evictCatalogCaches();
    return Result.success(Map.of("evicted", true));
  }
}
