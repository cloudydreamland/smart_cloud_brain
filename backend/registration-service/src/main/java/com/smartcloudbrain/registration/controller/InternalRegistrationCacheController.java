package com.smartcloudbrain.registration.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.registration.service.RegistrationSlotCacheService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/cache/slots")
public class InternalRegistrationCacheController {

  private final RegistrationSlotCacheService registrationSlotCacheService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalRegistrationCacheController(
      RegistrationSlotCacheService registrationSlotCacheService,
      InternalRequestGuard internalRequestGuard
  ) {
    this.registrationSlotCacheService = registrationSlotCacheService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @PostMapping("/evict")
  public Result<?> evictSlotsCache() {
    internalRequestGuard.requireServiceRequest();
    registrationSlotCacheService.evictSlotsCache();
    return Result.success(Map.of("evicted", true));
  }
}
