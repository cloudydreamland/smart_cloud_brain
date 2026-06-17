package com.smartcloudbrain.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiServiceConfig {

  @Bean
  AiProviderConfigurationValidator aiProviderConfigurationValidator(AiProviderProperties properties) {
    return new AiProviderConfigurationValidator(properties);
  }

  static class AiProviderConfigurationValidator {

    AiProviderConfigurationValidator(AiProviderProperties properties) {
      String provider = normalize(properties.provider());
      if ("mock".equals(provider)) {
        return;
      }
      if ("openai".equals(provider)) {
        require(properties.openai() == null ? "" : properties.openai().apiKey(), "OPENAI_API_KEY", provider);
        require(properties.openai() == null ? "" : properties.openai().baseUrl(), "OPENAI_BASE_URL", provider);
        require(properties.openai() == null ? "" : properties.openai().model(), "OPENAI_MODEL", provider);
        return;
      }
      if ("dify".equals(provider)) {
        require(properties.dify() == null ? "" : properties.dify().baseUrl(), "DIFY_BASE_URL", provider);
        require(properties.dify() == null ? "" : properties.dify().apiKey(), "DIFY_API_KEY", provider);
        return;
      }
      throw new IllegalStateException("Unsupported AI_PROVIDER: " + properties.provider());
    }

    private String normalize(String provider) {
      return provider == null || provider.isBlank() ? "openai" : provider.trim().toLowerCase();
    }

    private void require(String value, String name, String provider) {
      if (value == null || value.isBlank()) {
        throw new IllegalStateException(name + " is required when AI_PROVIDER=" + provider);
      }
    }
  }
}
