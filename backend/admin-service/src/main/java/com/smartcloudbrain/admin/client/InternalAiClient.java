package com.smartcloudbrain.admin.client;

import com.smartcloudbrain.aiapi.constant.AiInternalApi;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InternalAiClient {

  private static final ParameterizedTypeReference<Result<ScheduleSuggestResponse>> RESULT_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public InternalAiClient(
      RestClient.Builder restClientBuilder,
      @Value("${services.ai.base-url}") String baseUrl,
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String internalToken
  ) {
    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(InternalRequestGuard.INTERNAL_TOKEN_HEADER, internalToken)
        .build();
  }

  public ScheduleSuggestResponse suggestSchedule(ScheduleSuggestRequest request) {
    try {
      Result<ScheduleSuggestResponse> result = restClient.post()
          .uri(AiInternalApi.SCHEDULE_SUGGEST)
          .body(request)
          .retrieve()
          .body(RESULT_TYPE);
      if (result == null) {
        throw new BusinessException(500, "ai-service returned empty response");
      }
      if (result.code() != 0) {
        throw new BusinessException(result.code(), result.message());
      }
      if (result.data() == null) {
        throw new BusinessException(500, "ai-service returned empty schedule suggestion");
      }
      return result.data();
    } catch (RestClientException ex) {
      throw new BusinessException(500, "ai-service unavailable");
    }
  }
}
