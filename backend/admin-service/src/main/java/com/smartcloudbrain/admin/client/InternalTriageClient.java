package com.smartcloudbrain.admin.client;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InternalTriageClient {

  private static final ParameterizedTypeReference<Result<List<Map<String, Object>>>> LIST_RESULT =
      new ParameterizedTypeReference<>() {
      };
  private static final ParameterizedTypeReference<Result<Map<String, Object>>> MAP_RESULT =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public InternalTriageClient(
      RestClient.Builder restClientBuilder,
      @Value("${services.triage.base-url}") String baseUrl
  ) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
  }

  public List<Map<String, Object>> list() {
    try {
      return listData(restClient.get()
          .uri("/internal/triage-records/list")
          .retrieve()
          .body(LIST_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "triage-service unavailable");
    }
  }

  public Map<String, Object> detail(Long id) {
    try {
      return mapData(restClient.get()
          .uri(uriBuilder -> uriBuilder.path("/internal/triage-records/detail").queryParam("id", id).build())
          .retrieve()
          .body(MAP_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "triage-service unavailable");
    }
  }

  public Map<String, Object> assign(Long triageRecordId, Long doctorId) {
    try {
      return mapData(restClient.post()
          .uri("/internal/triage-records/assign")
          .body(Map.of("triageRecordId", triageRecordId, "doctorId", doctorId))
          .retrieve()
          .body(MAP_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "triage-service unavailable");
    }
  }

  public Map<String, Object> close(Long triageRecordId) {
    try {
      return mapData(restClient.post()
          .uri("/internal/triage-records/close")
          .body(Map.of("triageRecordId", triageRecordId))
          .retrieve()
          .body(MAP_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "triage-service unavailable");
    }
  }

  private List<Map<String, Object>> listData(Result<List<Map<String, Object>>> result) {
    if (result == null) {
      throw new BusinessException(500, "triage-service returned empty response");
    }
    if (result.code() != 0) {
      throw new BusinessException(result.code(), result.message());
    }
    return result.data() == null ? List.of() : result.data();
  }

  private Map<String, Object> mapData(Result<Map<String, Object>> result) {
    if (result == null) {
      throw new BusinessException(500, "triage-service returned empty response");
    }
    if (result.code() != 0) {
      throw new BusinessException(result.code(), result.message());
    }
    return result.data() == null ? Map.of() : result.data();
  }
}
