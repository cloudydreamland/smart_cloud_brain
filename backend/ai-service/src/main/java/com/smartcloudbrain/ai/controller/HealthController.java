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
        "model", aiProvider.modelName(),
        "openaiConfigured", openaiConfigured(),
        "difyConfigured", difyConfigured(),
        "openaiBaseUrl", properties.openai() == null ? "" : blankToDefault(properties.openai().baseUrl(), ""),
        "difyBaseUrl", properties.dify() == null ? "" : blankToDefault(properties.dify().baseUrl(), "")
    ));
  }

  private boolean openaiConfigured() {
    if (properties.openai() == null) {
      return false;
    }
    return hasText(properties.openai().baseUrl())
        && hasText(properties.openai().apiKey())
        && hasText(properties.openai().model());
  }

  @SuppressWarnings("deprecation")
  private boolean difyConfigured() {
    if (properties.dify() == null) {
      return false;
    }
    String fallbackKey = properties.dify().apiKey();
    return hasText(properties.dify().baseUrl())
        && hasTaskOrFallbackKey(properties.difyTriage(), fallbackKey)
        && hasTaskOrFallbackKey(properties.difyMedicalRecord(), fallbackKey)
        && hasTaskOrFallbackKey(properties.difyPrescriptionCheck(), fallbackKey)
        && hasTaskOrFallbackKey(properties.difySchedule(), fallbackKey);
  }

  private boolean hasTaskOrFallbackKey(AiProviderProperties.DifyWorkflow workflow, String fallbackKey) {
    return (workflow != null && hasText(workflow.apiKey())) || hasText(fallbackKey);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String blankToDefault(String value, String fallback) {
    return hasText(value) ? value : fallback;
  }
}
