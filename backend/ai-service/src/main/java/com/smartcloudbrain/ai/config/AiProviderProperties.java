package com.smartcloudbrain.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProviderProperties(
    String provider,
    String baseUrl,
    String apiKey,
    int timeoutMs,
    Dify dify
) {
  public record Dify(
      String baseUrl,
      String triageApiKey,
      String medicalRecordApiKey,
      String prescriptionCheckApiKey
  ) {
  }
}
