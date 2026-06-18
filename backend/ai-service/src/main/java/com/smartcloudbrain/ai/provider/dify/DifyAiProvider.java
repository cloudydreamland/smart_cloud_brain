package com.smartcloudbrain.ai.provider.dify;

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
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "dify")
public class DifyAiProvider implements AiProvider {

  enum WorkflowTask {
    TRIAGE,
    MEDICAL_RECORD,
    PRESCRIPTION_CHECK
  }

  private final AiProviderProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  public DifyAiProvider(AiProviderProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    int timeoutMs = properties.timeoutMs() <= 0 ? 8000 : properties.timeoutMs();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
    this.restClient = builder
        .baseUrl(normalizeBaseUrl(dify().baseUrl()))
        .requestFactory(requestFactory)
        .build();
  }

  @Override
  public String providerName() {
    return "dify";
  }

  @Override
  public String modelName() {
    return "dify-workflow";
  }

  @Override
  public TriageResponse triage(TriageRequest request, PromptResolveResponse prompt) {
    Map<String, Object> inputs = baseInputs(prompt);
    inputs.put("patientId", textInput(request.patientId()));
    inputs.put("chiefComplaint", textInput(request.chiefComplaint()));
    inputs.put("symptoms", textInput(request.symptoms()));
    inputs.put("age", textInput(request.age()));
    inputs.put("gender", textInput(request.gender()));
    inputs.put("allergyHistory", textInput(request.allergyHistory()));
    inputs.put("pastHistory", textInput(request.pastHistory()));
    JsonNode outputs = runWorkflow(inputs, "triage-" + textInput(request.patientId()), WorkflowTask.TRIAGE);
    return new TriageResponse(
        requiredText(outputs, "recommendedDepartment"),
        requiredText(outputs, "departmentCode"),
        text(outputs, "recommendedDoctorDirection", ""),
        text(outputs, "urgencyLevel", ""),
        optionalDouble(outputs, "confidence"),
        longList(outputs.get("recommendedDoctorIds")),
        requiredText(outputs, "reason"),
        false
    );
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request, PromptResolveResponse prompt) {
    Map<String, Object> inputs = baseInputs(prompt);
    inputs.put("registrationId", textInput(request.registrationId()));
    inputs.put("departmentCode", textInput(request.departmentCode()));
    inputs.put("dialogueText", textInput(request.dialogueText()));
    inputs.put("patientId", textInput(request.patientId()));
    inputs.put("patientName", textInput(request.patientName()));
    inputs.put("patientAge", textInput(request.patientAge()));
    inputs.put("patientGender", textInput(request.patientGender()));
    inputs.put("allergyHistory", textInput(request.allergyHistory()));
    inputs.put("pastHistory", textInput(request.pastHistory()));
    inputs.put("doctorId", textInput(request.doctorId()));
    inputs.put("doctorName", textInput(request.doctorName()));
    inputs.put("appointmentTime", textInput(request.appointmentTime()));
    JsonNode outputs = runWorkflow(inputs, "medical-record-" + textInput(request.registrationId()), WorkflowTask.MEDICAL_RECORD);
    return new MedicalRecordGenerateResponse(
        requiredText(outputs, "chiefComplaint"),
        requiredText(outputs, "presentIllness"),
        requiredText(outputs, "pastHistory"),
        text(outputs, "physicalExam", ""),
        requiredText(outputs, "diagnosis"),
        requiredText(outputs, "treatmentAdvice"),
        text(outputs, "soapContent", ""),
        false
    );
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request, PromptResolveResponse prompt) {
    Map<String, Object> inputs = baseInputs(prompt);
    inputs.put("patientId", textInput(request.patientId()));
    inputs.put("doctorId", textInput(request.doctorId()));
    inputs.put("medicalRecordId", textInput(request.medicalRecordId()));
    inputs.put("diagnosis", textInput(request.diagnosis()));
    inputs.put("patientAge", textInput(request.patientAge()));
    inputs.put("patientGender", textInput(request.patientGender()));
    inputs.put("allergyHistory", textInput(request.allergyHistory()));
    inputs.put("pastHistory", textInput(request.pastHistory()));
    inputs.put("drugs", drugTextInput(request.drugs()));
    JsonNode outputs = runWorkflow(inputs, "prescription-" + textInput(request.patientId()), WorkflowTask.PRESCRIPTION_CHECK);
    return new PrescriptionCheckResponse(
        requiredText(outputs, "riskLevel"),
        text(outputs, "riskDescription", ""),
        requiredText(outputs, "suggestions"),
        stringList(outputs.get("interactions")),
        stringList(outputs.get("contraindications")),
        stringList(outputs.get("adjustmentSuggestions")),
        false
    );
  }

  JsonNode parseOutputs(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode outputs = root.path("data").path("outputs");
      if (outputs.isMissingNode() || outputs.isNull()) {
        throw new IllegalArgumentException("Dify response missing data.outputs");
      }
      if (outputs.isTextual()) {
        return objectMapper.readTree(outputs.asText());
      }
      if (outputs.has("result") && outputs.get("result").isTextual()) {
        return objectMapper.readTree(outputs.get("result").asText());
      }
      return outputs;
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to parse Dify response", ex);
    }
  }

  private JsonNode runWorkflow(Map<String, Object> inputs, String user, WorkflowTask task) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("inputs", inputs);
    body.put("response_mode", "blocking");
    body.put("user", user == null || user.isBlank() ? "smart-cloud-brain" : "smart-cloud-brain-" + user);
    String response = restClient.post()
        .uri("/workflows/run")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + apiKeyFor(task))
        .body(body)
        .retrieve()
        .body(String.class);
    return parseOutputs(response);
  }

  private Map<String, Object> baseInputs(PromptResolveResponse prompt) {
    if (prompt == null || prompt.templateContent() == null || prompt.templateContent().isBlank()) {
      throw new IllegalStateException("Enabled prompt template is required");
    }
    Map<String, Object> inputs = new LinkedHashMap<>();
    inputs.put("promptTemplateName", textInput(prompt.templateName()));
    inputs.put("promptTemplate", prompt.templateContent());
    inputs.put("outputSchema", textInput(prompt.outputSchema()));
    return inputs;
  }

  private String drugTextInput(List<DrugItem> drugs) {
    if (drugs == null || drugs.isEmpty()) {
      return "";
    }
    List<String> lines = new ArrayList<>();
    for (DrugItem drug : drugs) {
      lines.add("drugName=%s, dosage=%s, usageMethod=%s, frequency=%s, days=%s, remark=%s".formatted(
          textInput(drug.drugName()),
          textInput(drug.dosage()),
          textInput(drug.usageMethod()),
          textInput(drug.frequency()),
          textInput(drug.days()),
          textInput(drug.remark())
      ));
    }
    return String.join("\n", lines);
  }

  private String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? fallback : value.asText(fallback);
  }

  private String requiredText(JsonNode node, String field) {
    String value = text(node, field, "");
    if (value.isBlank()) {
      throw new IllegalStateException("AI response missing required field: " + field);
    }
    return value;
  }

  private Double optionalDouble(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || !value.isNumber() ? null : value.asDouble();
  }

  private List<String> stringList(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (node == null || node.isNull()) {
      return values;
    }
    if (node.isArray()) {
      node.forEach(item -> values.add(item.asText()));
    } else if (!node.asText("").isBlank()) {
      values.add(node.asText());
    }
    return values;
  }

  private List<Long> longList(JsonNode node) {
    List<Long> values = new ArrayList<>();
    if (node == null || node.isNull()) {
      return values;
    }
    if (node.isArray()) {
      node.forEach(item -> values.add(item.asLong()));
    } else if (node.canConvertToLong()) {
      values.add(node.asLong());
    }
    return values;
  }

  private AiProviderProperties.Dify dify() {
    if (properties.dify() == null) {
      throw new IllegalStateException("dify provider configuration is required");
    }
    return properties.dify();
  }

  @SuppressWarnings("deprecation")
  String apiKeyFor(WorkflowTask task) {
    AiProviderProperties.DifyWorkflow workflow = switch (task) {
      case TRIAGE -> properties.difyTriage();
      case MEDICAL_RECORD -> properties.difyMedicalRecord();
      case PRESCRIPTION_CHECK -> properties.difyPrescriptionCheck();
    };
    String taskKey = workflow == null ? "" : workflow.apiKey();
    if (taskKey != null && !taskKey.isBlank()) {
      return taskKey;
    }
    return require(dify().apiKey(), keyName(task) + " or DIFY_API_KEY");
  }

  private String keyName(WorkflowTask task) {
    return switch (task) {
      case TRIAGE -> "DIFY_TRIAGE_API_KEY";
      case MEDICAL_RECORD -> "DIFY_MEDICAL_RECORD_API_KEY";
      case PRESCRIPTION_CHECK -> "DIFY_PRESCRIPTION_CHECK_API_KEY";
    };
  }

  private String normalizeBaseUrl(String value) {
    String baseUrl = require(value, "DIFY_BASE_URL").trim();
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private String require(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required when AI_PROVIDER=dify");
    }
    return value;
  }

  private String textInput(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
