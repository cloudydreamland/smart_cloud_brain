package com.smartcloudbrain.ai.controller;

import com.smartcloudbrain.common.result.Result;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/internal/ai/health")
  public Result<?> health() {
    return Result.success(Map.of("service", "ai-service", "status", "UP"));
  }
}
