package com.smartcloudbrain.doctor.client;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InternalRegistrationCacheClient {

  private static final Logger log = LoggerFactory.getLogger(InternalRegistrationCacheClient.class);
  private static final ParameterizedTypeReference<Result<Object>> RESULT =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public InternalRegistrationCacheClient(
      RestClient.Builder restClientBuilder,
      @Value("${services.registration.base-url}") String baseUrl,
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String internalToken
  ) {
    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(InternalRequestGuard.INTERNAL_TOKEN_HEADER, internalToken)
        .build();
  }

  public void evictSlotsCache() {
    try {
      Result<Object> result = restClient.post()
          .uri("/internal/cache/slots/evict")
          .retrieve()
          .body(RESULT);
      if (result == null) {
        log.warn("registration-service slots cache evict returned empty response");
      } else if (result.code() != 0) {
        log.warn("registration-service slots cache evict failed: {}", result.message());
      }
    } catch (RestClientException exception) {
      log.warn("registration-service slots cache evict request failed: {}", exception.getMessage());
    }
  }
}
