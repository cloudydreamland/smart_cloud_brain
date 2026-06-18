package com.smartcloudbrain.ai.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiServiceConfigTest {

  @Test
  void allowsMockWithoutExternalKeys() {
    assertDoesNotThrow(() -> validator(properties("mock", "", "", "", "")));
  }

  @Test
  void acceptsThreeTaskSpecificDifyKeys() {
    assertDoesNotThrow(() -> validator(properties("dify", "", "triage", "medical", "prescription")));
  }

  @Test
  void acceptsDeprecatedSharedDifyKeyAsFallback() {
    assertDoesNotThrow(() -> validator(properties("dify", "legacy", "", "", "")));
  }

  @Test
  void rejectsDifyWhenOneTaskHasNoKeyOrFallback() {
    assertThrows(
        IllegalStateException.class,
        () -> validator(properties("dify", "", "triage", "medical", ""))
    );
  }

  private AiServiceConfig.AiProviderConfigurationValidator validator(AiProviderProperties properties) {
    return new AiServiceConfig.AiProviderConfigurationValidator(properties);
  }

  private AiProviderProperties properties(
      String provider,
      String legacy,
      String triage,
      String medical,
      String prescription
  ) {
    return new AiProviderProperties(
        provider,
        8000,
        null,
        new AiProviderProperties.Dify("http://localhost/v1", legacy),
        new AiProviderProperties.DifyWorkflow(triage),
        new AiProviderProperties.DifyWorkflow(medical),
        new AiProviderProperties.DifyWorkflow(prescription)
    );
  }
}
