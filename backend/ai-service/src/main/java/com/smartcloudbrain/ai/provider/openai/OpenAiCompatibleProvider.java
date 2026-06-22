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
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.time.Duration;
import java.time.LocalDate;
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
        .baseUrl(require(openAi().baseUrl(), "OPENAI_BASE_URL"))
        .requestFactory(requestFactory)
        .defaultHeader("Authorization", "Bearer " + require(openAi().apiKey(), "OPENAI_API_KEY"))
        .build();
    this.concurrencyLimiter = new Semaphore(8);
  }

  @Override
  public String providerName() {
    return "openai";
  }

  @Override
  public String modelName() {
    return openAi().model();
  }

  @Override
  public TriageResponse triage(TriageRequest request, PromptResolveResponse prompt) {
    JsonNode json = completeJson(prompt, """
        Return JSON only with keys recommendedDepartment, departmentCode, recommendedDoctorDirection, urgencyLevel, confidence, recommendedDoctorIds, reason.
        Patient context:
        - patientId: %s
        - age: %s
        - gender: %s
        - chiefComplaint: %s
        - symptoms: %s
        - allergyHistory: %s
        - pastHistory: %s
        """.formatted(
        textInput(request.patientId()),
        textInput(request.age()),
        textInput(request.gender()),
        textInput(request.chiefComplaint()),
        textInput(request.symptoms()),
        textInput(request.allergyHistory()),
        textInput(request.pastHistory())
    ));
    return new TriageResponse(
        requiredText(json, "recommendedDepartment"),
        requiredText(json, "departmentCode"),
        requiredText(json, "recommendedDoctorDirection"),
        requiredText(json, "urgencyLevel"),
        requiredDouble(json, "confidence"),
        longList(json.path("recommendedDoctorIds")),
        requiredText(json, "reason"),
        false
    );
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request, PromptResolveResponse prompt) {
    JsonNode json = completeJson(prompt, """
        Return JSON only with keys chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis, treatmentAdvice, soapContent.
        Department: %s
        Registration id: %s
        Appointment time: %s
        Patient: %s, age %s, gender %s
        Allergy history: %s
        Past history: %s
        Doctor id: %s
        Dialogue: %s
        """.formatted(
        textInput(request.departmentCode()),
        textInput(request.registrationId()),
        textInput(request.appointmentTime()),
        textInput(request.patientName()),
        textInput(request.patientAge()),
        textInput(request.patientGender()),
        textInput(request.allergyHistory()),
        textInput(request.pastHistory()),
        textInput(request.doctorId()),
        textInput(request.dialogueText())
    ));
    return new MedicalRecordGenerateResponse(
        requiredText(json, "chiefComplaint"),
        requiredText(json, "presentIllness"),
        requiredText(json, "pastHistory"),
        text(json, "physicalExam", ""),
        requiredText(json, "diagnosis"),
        requiredText(json, "treatmentAdvice"),
        text(json, "soapContent", ""),
        false
    );
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request, PromptResolveResponse prompt) {
    String drugs = request.drugs().stream()
        .map(this::drugLine)
        .toList().toString();
    JsonNode json = completeJson(prompt, """
        Return JSON only with keys riskLevel, riskDescription, suggestions, interactions, contraindications, adjustmentSuggestions.
        Patient id: %s
        Doctor id: %s
        Diagnosis: %s
        Patient age: %s
        Patient gender: %s
        Allergy history: %s
        Past history: %s
        Drugs: %s
        """.formatted(
        textInput(request.patientId()),
        textInput(request.doctorId()),
        textInput(request.diagnosis()),
        textInput(request.patientAge()),
        textInput(request.patientGender()),
        textInput(request.allergyHistory()),
        textInput(request.pastHistory()),
        drugs
    ));
    return new PrescriptionCheckResponse(
        requiredText(json, "riskLevel"),
        requiredText(json, "riskDescription"),
        requiredText(json, "suggestions"),
        stringList(json.path("interactions")),
        stringList(json.path("contraindications")),
        stringList(json.path("adjustmentSuggestions")),
        false
    );
  }

  @Override
  public ScheduleSuggestResponse suggestSchedule(ScheduleSuggestRequest request, PromptResolveResponse prompt) {
    JsonNode json = completeJson(prompt, """
        Return JSON only with a suggestions array. Every suggestion must contain doctorId, departmentId, workDate (yyyy-MM-dd), timeRange (HH:mm-HH:mm), capacity (1-100), and reason.
        Start date: %s
        Days: %s
        Enabled doctor candidates: %s
        Departments: %s
        Existing schedules that must not be duplicated: %s
        """.formatted(
        request.startDate(),
        request.days(),
        jsonText(request.doctors()),
        jsonText(request.departments()),
        jsonText(request.existingSchedules())
    ));
    JsonNode items = json.path("suggestions");
    if (!items.isArray()) {
      throw new IllegalStateException("AI response missing required array: suggestions");
    }
    List<ScheduleSuggestionItem> suggestions = new ArrayList<>();
    for (JsonNode item : items) {
      suggestions.add(new ScheduleSuggestionItem(
          requiredLong(item, "doctorId"),
          requiredLong(item, "departmentId"),
          LocalDate.parse(requiredText(item, "workDate")),
          requiredText(item, "timeRange"),
          requiredInt(item, "capacity"),
          requiredText(item, "reason")
      ));
    }
    return new ScheduleSuggestResponse(suggestions, "openai", false);
  }

  private JsonNode completeJson(PromptResolveResponse prompt, String userContent) {
    return withPermit(() -> {
      RuntimeException last = null;
      int attempts = 2;
      for (int i = 0; i < attempts; i++) {
        try {
          String content = chatCompletion(prompt, userContent);
          return objectMapper.readTree(stripJson(content));
        } catch (RuntimeException ex) {
          last = ex;
        } catch (Exception ex) {
          last = new IllegalStateException("AI response JSON parse failed", ex);
        }
      }
      throw last == null ? new IllegalStateException("AI provider failed") : last;
    });
  }

  private String chatCompletion(PromptResolveResponse prompt, String userContent) {
    if (prompt == null || prompt.templateContent() == null || prompt.templateContent().isBlank()) {
      throw new IllegalStateException("Enabled prompt template is required");
    }
    Map<String, Object> body = Map.of(
        "model", model(),
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
            Map.of("role", "system", "content", prompt.templateContent() + "\nOutput schema: " + textInput(prompt.outputSchema()) + "\nReturn compact JSON only."),
            Map.of("role", "user", "content", userContent)
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
    return require(openAi().model(), "OPENAI_MODEL");
  }

  private String text(JsonNode json, String field, String fallback) {
    String value = json.path(field).asText();
    return value == null || value.isBlank() ? fallback : value;
  }

  private String requiredText(JsonNode json, String field) {
    String value = json.path(field).asText();
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("AI response missing required field: " + field);
    }
    return value;
  }

  private Double requiredDouble(JsonNode json, String field) {
    if (!json.has(field) || !json.get(field).isNumber()) {
      throw new IllegalStateException("AI response missing numeric field: " + field);
    }
    return json.get(field).asDouble();
  }

  private long requiredLong(JsonNode json, String field) {
    if (!json.has(field) || !json.get(field).canConvertToLong()) {
      throw new IllegalStateException("AI response missing numeric field: " + field);
    }
    return json.get(field).asLong();
  }

  private int requiredInt(JsonNode json, String field) {
    if (!json.has(field) || !json.get(field).canConvertToInt()) {
      throw new IllegalStateException("AI response missing numeric field: " + field);
    }
    return json.get(field).asInt();
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

  private AiProviderProperties.OpenAi openAi() {
    if (properties.openai() == null) {
      throw new IllegalStateException("openai provider configuration is required");
    }
    return properties.openai();
  }

  private String textInput(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private String jsonText(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to serialize AI input", ex);
    }
  }

  private String drugLine(DrugItem drug) {
    return "drugName=%s, dosage=%s, usageMethod=%s, frequency=%s, days=%s, remark=%s".formatted(
        textInput(drug.drugName()),
        textInput(drug.dosage()),
        textInput(drug.usageMethod()),
        textInput(drug.frequency()),
        textInput(drug.days()),
        textInput(drug.remark())
    );
  }

  private String stripJson(String content) {
    String text = content == null ? "" : content.trim();
    if (text.startsWith("```")) {
      int start = text.indexOf('{');
      int end = text.lastIndexOf('}');
      if (start >= 0 && end > start) {
        return text.substring(start, end + 1);
      }
    }
    return text;
  }

  @FunctionalInterface
  private interface SupplierWithRuntimeException<T> {
    T get();
  }
}
