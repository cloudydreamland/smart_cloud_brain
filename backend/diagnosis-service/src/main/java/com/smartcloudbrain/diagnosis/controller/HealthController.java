package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.common.result.Result;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/api/health")
  public Result<?> health() {
    return Result.success(Map.of("service", "diagnosis-service", "status", "UP"));
  }
}
