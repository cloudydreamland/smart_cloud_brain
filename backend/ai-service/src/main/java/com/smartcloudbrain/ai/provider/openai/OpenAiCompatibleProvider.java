package com.smartcloudbrain.ai.provider.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.config.AiProviderProperties;
import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai")
public class OpenAiCompatibleProvider implements AiProvider {

  private final AiProviderProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final Semaphore concurrencyLimiter;

  public OpenAiCompatibleProvider(AiProviderProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
    requestFactory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));
    this.restClient = RestClient.builder()
        .baseUrl(require(properties.baseUrl(), "AI_BASE_URL"))
        .requestFactory(requestFactory)
        .defaultHeader("Authorization", "Bearer " + require(properties.apiKey(), "AI_API_KEY"))
        .build();
    this.concurrencyLimiter = new Semaphore(8);
  }

  @Override
  public TriageResponse triage(TriageRequest request) {
    JsonNode json = completeJson("""
        Return JSON only with keys recommendedDepartment, departmentCode, recommendedDoctorIds, reason.
        Chief complaint: %s
        """.formatted(request.chiefComplaint()));
    return new TriageResponse(
        text(json, "recommendedDepartment", "General Practice"),
        text(json, "departmentCode", "GENERAL"),
        longList(json.path("recommendedDoctorIds")),
        text(json, "reason", "AI triage completed."),
        false
    );
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    JsonNode json = completeJson("""
        Return JSON only with keys chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis, treatmentAdvice.
        Department: %s
        Dialogue: %s
        """.formatted(request.departmentCode(), request.dialogueText()));
    return new MedicalRecordGenerateResponse(
        text(json, "chiefComplaint", "Chief complaint pending review."),
        text(json, "presentIllness", "Present illness pending review."),
        text(json, "pastHistory", "Past history pending review."),
        text(json, "physicalExam", "Physical exam pending review."),
        text(json, "diagnosis", "Diagnosis pending doctor confirmation."),
        text(json, "treatmentAdvice", "Treatment advice pending doctor confirmation."),
        false
    );
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    String drugs = request.drugs().stream()
        .map(DrugItem::drugName)
        .toList()
        .toString();
    JsonNode json = completeJson("""
        Return JSON only with keys riskLevel, suggestions, interactions.
        Patient id: %s
        Doctor id: %s
        Drugs: %s
        """.formatted(request.patientId(), request.doctorId(), drugs));
    return new PrescriptionCheckResponse(
        text(json, "riskLevel", "UNKNOWN"),
        text(json, "suggestions", "Review required."),
        stringList(json.path("interactions")),
        false
    );
  }

  private JsonNode completeJson(String prompt) {
    return withPermit(() -> {
      RuntimeException last = null;
      int attempts = 2;
      for (int i = 0; i < attempts; i++) {
        try {
          String content = chatCompletion(prompt);
          return objectMapper.readTree(content);
        } catch (RuntimeException ex) {
          last = ex;
        } catch (Exception ex) {
          last = new IllegalStateException("AI response JSON parse failed", ex);
        }
      }
      throw last == null ? new IllegalStateException("AI provider failed") : last;
    });
  }

  private String chatCompletion(String prompt) {
    Map<String, Object> body = Map.of(
        "model", model(),
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
            Map.of("role", "system", "content", "You are a clinical assistant. Return compact JSON only."),
            Map.of("role", "user", "content", prompt)
        )
    );
    JsonNode response = restClient.post()
        .uri("/chat/completions")
        .body(body)
        .retrieve()
        .body(JsonNode.class);
    if (response == null) {
      throw new IllegalStateException("AI provider returned empty response");
    }
    return response.path("choices").path(0).path("message").path("content").asText();
  }

  private <T> T withPermit(SupplierWithRuntimeException<T> supplier) {
    boolean acquired = false;
    try {
      concurrencyLimiter.acquire();
      acquired = true;
      return supplier.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("AI request interrupted", ex);
    } finally {
      if (acquired) {
        concurrencyLimiter.release();
      }
    }
  }

  private String model() {
    return "gpt-4o-mini";
  }

  private String text(JsonNode json, String field, String fallback) {
    String value = json.path(field).asText();
    return value == null || value.isBlank() ? fallback : value;
  }

  private List<Long> longList(JsonNode json) {
    List<Long> values = new ArrayList<>();
    if (json != null && json.isArray()) {
      for (JsonNode item : json) {
        values.add(item.asLong());
      }
    }
    return values;
  }

  private List<String> stringList(JsonNode json) {
    List<String> values = new ArrayList<>();
    if (json != null && json.isArray()) {
      for (JsonNode item : json) {
        values.add(item.asText());
      }
    }
    return values;
  }

  private String require(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required for openai provider");
    }
    return value;
  }

  @FunctionalInterface
  private interface SupplierWithRuntimeException<T> {
    T get();
  }
}
