package com.smartcloudbrain.auth.client;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InternalNotificationClient {

  private static final ParameterizedTypeReference<Result<Map<String, Object>>> MAP_RESULT =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public InternalNotificationClient(
      RestClient.Builder restClientBuilder,
      @Value("${services.notification.base-url}") String baseUrl,
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String internalToken
  ) {
    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(InternalRequestGuard.INTERNAL_TOKEN_HEADER, internalToken)
        .build();
  }

  public Map<String, Object> sendEmail(String toAddress, String subject, String content) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("toAddress", toAddress);
    body.put("subject", subject);
    body.put("content", content);
    try {
      Result<Map<String, Object>> result = restClient.post()
          .uri("/internal/notifications/email")
          .body(body)
          .retrieve()
          .body(MAP_RESULT);
      if (result == null) {
        throw new BusinessException(500, "notification-service returned empty response");
      }
      if (result.code() != 0) {
        throw new BusinessException(result.code(), result.message());
      }
      return result.data() == null ? Map.of() : result.data();
    } catch (RestClientException ex) {
      throw new BusinessException(500, "notification-service unavailable");
    }
  }
}
