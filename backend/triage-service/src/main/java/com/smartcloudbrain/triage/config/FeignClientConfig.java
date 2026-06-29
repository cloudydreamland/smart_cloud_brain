package com.smartcloudbrain.triage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;

@Configuration
public class FeignClientConfig {

  @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}")
  private String internalToken;

  @Bean
  public RequestInterceptor internalServiceTokenInterceptor() {
    return template -> {
      template.header("X-Internal-Service-Token", internalToken);
    };
  }
}
