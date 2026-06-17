package com.smartcloudbrain.ai.controller;

import com.smartcloudbrain.ai.config.AiProviderProperties;
import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.common.result.Result;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  private final AiProvider aiProvider;
  private final AiProviderProperties properties;

  public HealthController(AiProvider aiProvider, AiProviderProperties properties) {
    this.aiProvider = aiProvider;
    this.properties = properties;
  }

  @GetMapping("/internal/ai/health")
  public Result<?> health() {
    return Result.success(Map.of(
        "service", "ai-service",
        "status", "UP",
        "provider", aiProvider.providerName(),
        "difyConfigured", difyConfigured(),
        "difyBaseUrl", properties.dify() == null ? "" : blankToDefault(properties.dify().baseUrl(), "http://localhost/v1")
    ));
  }

  private boolean difyConfigured() {
    if (properties.dify() == null) {
      return false;
    }
    return hasText(properties.dify().baseUrl())
        && hasText(properties.dify().triageApiKey())
        && hasText(properties.dify().medicalRecordApiKey())
        && hasText(properties.dify().prescriptionCheckApiKey());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String blankToDefault(String value, String fallback) {
    return hasText(value) ? value : fallback;
  }
}
