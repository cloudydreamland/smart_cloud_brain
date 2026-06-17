package com.smartcloudbrain.prescription.config;

import com.smartcloudbrain.common.security.InternalRequestGuard;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

  @Bean
  RequestInterceptor internalTokenRequestInterceptor(
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String internalToken
  ) {
    return template -> template.header(InternalRequestGuard.INTERNAL_TOKEN_HEADER, internalToken);
  }
}
