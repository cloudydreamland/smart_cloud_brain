package com.smartcloudbrain.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProviderProperties(
    String provider,
    int timeoutMs,
    OpenAi openai,
    Dify dify,
    DifyWorkflow difyTriage,
    DifyWorkflow difyMedicalRecord,
    DifyWorkflow difyPrescriptionCheck
) {
  public record OpenAi(
      String baseUrl,
      String apiKey,
      String model
  ) {
  }

  public record Dify(
      String baseUrl,
      String apiKey
  ) {
    @Deprecated
    public String apiKey() {
      return apiKey;
    }
  }

  public record DifyWorkflow(
      String apiKey
  ) {
  }
}
