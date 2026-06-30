package com.smartcloudbrain.admin.client;

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
public class InternalPatientCacheClient {

  private static final Logger log = LoggerFactory.getLogger(InternalPatientCacheClient.class);
  private static final ParameterizedTypeReference<Result<Object>> RESULT =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public InternalPatientCacheClient(
      RestClient.Builder restClientBuilder,
      @Value("${services.patient.base-url}") String baseUrl,
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String internalToken
  ) {
    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(InternalRequestGuard.INTERNAL_TOKEN_HEADER, internalToken)
        .build();
  }

  public void evictCatalogCache() {
    try {
      Result<Object> result = restClient.post()
          .uri("/internal/cache/catalog/evict")
          .retrieve()
          .body(RESULT);
      if (result == null) {
        log.warn("patient-service catalog cache evict returned empty response");
      } else if (result.code() != 0) {
        log.warn("patient-service catalog cache evict failed: {}", result.message());
      }
    } catch (RestClientException exception) {
      log.warn("patient-service catalog cache evict request failed: {}", exception.getMessage());
    }
  }
}
