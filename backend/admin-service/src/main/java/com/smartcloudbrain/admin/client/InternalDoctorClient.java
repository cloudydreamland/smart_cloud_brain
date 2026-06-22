package com.smartcloudbrain.admin.client;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InternalDoctorClient {

  private static final ParameterizedTypeReference<Result<List<Map<String, Object>>>> LIST_RESULT =
      new ParameterizedTypeReference<>() {
      };
  private static final ParameterizedTypeReference<Result<Map<String, Object>>> MAP_RESULT =
      new ParameterizedTypeReference<>() {
      };

  private final RestClient restClient;

  public InternalDoctorClient(
      RestClient.Builder restClientBuilder,
      @Value("${services.doctor.base-url}") String baseUrl,
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String internalToken
  ) {
    this.restClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(InternalRequestGuard.INTERNAL_TOKEN_HEADER, internalToken)
        .build();
  }

  public List<Map<String, Object>> schedules() {
    return schedules(null, null, null, null, null);
  }

  public List<Map<String, Object>> schedules(String startDate, String endDate, Long departmentId, Long doctorId, String status) {
    try {
      return data(restClient.get()
          .uri(uriBuilder -> uriBuilder.path("/internal/doctors/schedules/list")
              .queryParamIfPresent("startDate", java.util.Optional.ofNullable(startDate).filter(value -> !value.isBlank()))
              .queryParamIfPresent("endDate", java.util.Optional.ofNullable(endDate).filter(value -> !value.isBlank()))
              .queryParamIfPresent("departmentId", java.util.Optional.ofNullable(departmentId))
              .queryParamIfPresent("doctorId", java.util.Optional.ofNullable(doctorId))
              .queryParamIfPresent("status", java.util.Optional.ofNullable(status).filter(value -> !value.isBlank()))
              .build())
          .retrieve()
          .body(LIST_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "doctor-service unavailable");
    }
  }

  public List<Map<String, Object>> publishSchedules(List<Map<String, Object>> schedules) {
    try {
      return data(restClient.post()
          .uri("/internal/doctors/schedules/publish")
          .body(Map.of("schedules", schedules))
          .retrieve()
          .body(LIST_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "doctor-service unavailable");
    }
  }

  public Map<String, Object> saveSchedule(Map<String, Object> schedule) {
    try {
      return mapData(restClient.post()
          .uri("/internal/doctors/schedules/save")
          .body(schedule)
          .retrieve()
          .body(MAP_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "doctor-service unavailable");
    }
  }

  public Map<String, Object> cancelSchedule(Long scheduleId) {
    try {
      return mapData(restClient.post()
          .uri("/internal/doctors/schedules/cancel")
          .body(Map.of("scheduleId", scheduleId))
          .retrieve()
          .body(MAP_RESULT));
    } catch (RestClientException ex) {
      throw new BusinessException(500, "doctor-service unavailable");
    }
  }

  private List<Map<String, Object>> data(Result<List<Map<String, Object>>> result) {
    if (result == null) {
      throw new BusinessException(500, "doctor-service returned empty response");
    }
    if (result.code() != 0) {
      throw new BusinessException(result.code(), result.message());
    }
    return result.data() == null ? List.of() : result.data();
  }

  private Map<String, Object> mapData(Result<Map<String, Object>> result) {
    if (result == null) {
      throw new BusinessException(500, "doctor-service returned empty response");
    }
    if (result.code() != 0) {
      throw new BusinessException(result.code(), result.message());
    }
    return result.data() == null ? Map.of() : result.data();
  }
}
