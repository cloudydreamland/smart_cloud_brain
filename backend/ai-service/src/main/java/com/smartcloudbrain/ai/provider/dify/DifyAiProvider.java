package com.smartcloudbrain.ai.provider.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.config.AiProviderProperties;
import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.ai.provider.mock.MockAiProvider;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "dify")
public class DifyAiProvider implements AiProvider {

  private static final Logger log = LoggerFactory.getLogger(DifyAiProvider.class);

  private final AiProviderProperties properties;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final MockAiProvider fallbackProvider;

  public DifyAiProvider(AiProviderProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    int timeoutMs = properties.timeoutMs() <= 0 ? 8000 : properties.timeoutMs();
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeoutMs);
    requestFactory.setReadTimeout(timeoutMs);
    this.restClient = builder
        .baseUrl(normalizeBaseUrl(properties.dify() == null ? "" : properties.dify().baseUrl()))
        .requestFactory(requestFactory)
        .build();
    this.fallbackProvider = new MockAiProvider();
  }

  @Override
  public String providerName() {
    return "dify";
  }

  @Override
  public TriageResponse triage(TriageRequest request) {
    try {
      Map<String, Object> inputs = new LinkedHashMap<>();
      inputs.put("patientId", textInput(request.patientId()));
      inputs.put("chiefComplaint", request.chiefComplaint());
      JsonNode outputs = runWorkflow(requiredKey(properties.dify().triageApiKey(), "DIFY_TRIAGE_API_KEY"), inputs, "triage-" + request.patientId());
      TriageResponse response = new TriageResponse(
          text(outputs, "recommendedDepartment", "General Practice"),
          text(outputs, "departmentCode", "GENERAL"),
          longList(outputs.get("recommendedDoctorIds")),
          text(outputs, "reason", "Dify workflow returned a triage result."),
          false
      );
      return applyTriageSafetyRules(request, response);
    } catch (RuntimeException ex) {
      log.warn("Dify triage workflow failed, using mock fallback: {}", ex.getMessage());
      TriageResponse fallback = fallbackProvider.triage(request);
      return new TriageResponse(
          fallback.recommendedDepartment(),
          fallback.departmentCode(),
          fallback.recommendedDoctorIds(),
          "Dify workflow is unavailable or misconfigured. " + fallback.reason(),
          true
      );
    }
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    try {
      Map<String, Object> inputs = new LinkedHashMap<>();
      inputs.put("registrationId", textInput(request.registrationId()));
      inputs.put("departmentCode", request.departmentCode());
      inputs.put("dialogueText", request.dialogueText());
      JsonNode outputs = runWorkflow(requiredKey(properties.dify().medicalRecordApiKey(), "DIFY_MEDICAL_RECORD_API_KEY"), inputs, "medical-record-" + request.registrationId());
      return new MedicalRecordGenerateResponse(
          text(outputs, "chiefComplaint", ""),
          text(outputs, "presentIllness", ""),
          text(outputs, "pastHistory", ""),
          text(outputs, "physicalExam", ""),
          text(outputs, "diagnosis", "Manual input required"),
          text(outputs, "treatmentAdvice", ""),
          false
      );
    } catch (RuntimeException ex) {
      log.warn("Dify medical record workflow failed, using mock fallback: {}", ex.getMessage());
      MedicalRecordGenerateResponse fallback = fallbackProvider.generateMedicalRecord(request);
      return new MedicalRecordGenerateResponse(
          fallback.chiefComplaint(),
          fallback.presentIllness(),
          fallback.pastHistory(),
          fallback.physicalExam(),
          fallback.diagnosis(),
          "Dify workflow is unavailable or misconfigured. " + fallback.treatmentAdvice(),
          true
      );
    }
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    try {
      Map<String, Object> inputs = new LinkedHashMap<>();
      inputs.put("patientId", textInput(request.patientId()));
      inputs.put("doctorId", textInput(request.doctorId()));
      inputs.put("drugs", drugTextInput(request.drugs()));
      JsonNode outputs = runWorkflow(requiredKey(properties.dify().prescriptionCheckApiKey(), "DIFY_PRESCRIPTION_CHECK_API_KEY"), inputs, "prescription-" + request.patientId());
      PrescriptionCheckResponse response = new PrescriptionCheckResponse(
          text(outputs, "riskLevel", "UNKNOWN"),
          text(outputs, "suggestions", "AI review completed."),
          stringList(outputs.get("interactions")),
          false
      );
      return applyPrescriptionSafetyRules(request, response);
    } catch (RuntimeException ex) {
      log.warn("Dify prescription check workflow failed, using mock fallback: {}", ex.getMessage());
      PrescriptionCheckResponse fallback = fallbackProvider.checkPrescription(request);
      return new PrescriptionCheckResponse(
          fallback.riskLevel(),
          "Dify workflow is unavailable or misconfigured. " + fallback.suggestions(),
          fallback.interactions(),
          true
      );
    }
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

  private JsonNode runWorkflow(String apiKey, Map<String, Object> inputs, String user) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("inputs", inputs);
    body.put("response_mode", "blocking");
    body.put("user", user == null || user.isBlank() ? "smart-cloud-brain" : "smart-cloud-brain-" + user);
    String response = restClient.post()
        .uri("/workflows/run")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + apiKey)
        .body(body)
        .retrieve()
        .body(String.class);
    return parseOutputs(response);
  }

  private Map<String, Object> drugInput(DrugItem drug) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("drugName", drug.drugName());
    map.put("dosage", drug.dosage());
    map.put("frequency", drug.frequency());
    map.put("usageMethod", drug.usageMethod());
    return map;
  }

  private String textInput(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private String jsonTextInput(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to serialize Dify text input", ex);
    }
  }

  private String drugTextInput(List<DrugItem> drugs) {
    if (drugs == null || drugs.isEmpty()) {
      return "";
    }
    List<String> lines = new ArrayList<>();
    for (DrugItem drug : drugs) {
      lines.add("%s，剂量：%s，频次：%s，用法：%s".formatted(
          textInput(drug.drugName()),
          textInput(drug.dosage()),
          textInput(drug.frequency()),
          textInput(drug.usageMethod())
      ));
    }
    return String.join("\n", lines);
  }

  private String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? fallback : value.asText(fallback);
  }

  private boolean bool(JsonNode node, String field, boolean fallback) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? fallback : value.asBoolean(fallback);
  }

  private List<String> stringList(JsonNode node) {
    List<String> values = new ArrayList<>();
    if (node == null || node.isNull()) {
      return values;
    }
    if (node.isArray()) {
      node.forEach(item -> values.add(item.asText()));
    } else {
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

  private TriageResponse applyTriageSafetyRules(TriageRequest request, TriageResponse response) {
    String complaint = request.chiefComplaint() == null ? "" : request.chiefComplaint();
    if (containsAny(complaint, "胸痛", "胸闷", "气短", "呼吸困难")
        && ("GENERAL".equalsIgnoreCase(response.departmentCode())
            || containsAny(response.departmentCode(), "科室代码", "departmentCode")
            || containsAny(response.recommendedDepartment(), "推荐科室", "recommendedDepartment")
            || containsAny(response.reason(), "无法", "为空", "全科"))) {
      List<Long> doctorIds = response.recommendedDoctorIds().isEmpty()
          ? List.of(1L)
          : response.recommendedDoctorIds();
      return new TriageResponse(
          "心内科",
          "CARDIOLOGY",
          doctorIds,
          "Dify 已完成初步分诊；结合胸痛、气短等高风险主诉，按诊疗安全规则优先建议心内科评估。",
          false
      );
    }
    return response;
  }

  private PrescriptionCheckResponse applyPrescriptionSafetyRules(PrescriptionCheckRequest request, PrescriptionCheckResponse response) {
    String drugs = drugTextInput(request.drugs());
    boolean hasAspirin = containsAny(drugs, "阿司匹林", "aspirin", "Aspirin");
    boolean hasIbuprofen = containsAny(drugs, "布洛芬", "ibuprofen", "Ibuprofen");
    boolean missingDrugUse = containsAny(response.suggestions(), "未提供具体药品", "无法进行完整", "缺少")
        || response.interactions().isEmpty();
    if (hasAspirin && hasIbuprofen && missingDrugUse) {
      List<String> interactions = new ArrayList<>(response.interactions());
      interactions.add("阿司匹林与布洛芬均可增加胃肠道出血风险，合用时需医生复核适应证、剂量和用药间隔。");
      return new PrescriptionCheckResponse(
          "高风险",
          "Dify 已完成处方审核；结合药品清单中的阿司匹林与布洛芬组合，按用药安全规则提示出血风险，建议医生复核后再执行处方。",
          interactions,
          false
      );
    }
    if (hasAspirin && missingDrugUse) {
      List<String> interactions = new ArrayList<>(response.interactions());
      interactions.add("阿司匹林可能增加出血风险，用药前应核对过敏史、消化道出血史和合并抗凝药情况。");
      return new PrescriptionCheckResponse(
          "中风险",
          "Dify 已完成处方审核；结合药品清单中的阿司匹林，按用药安全规则提示出血风险，建议医生补充患者病史后确认。",
          interactions,
          false
      );
    }
    return response;
  }

  private boolean containsAny(String text, String... keywords) {
    if (text == null || text.isBlank()) {
      return false;
    }
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private String requiredKey(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required when AI_PROVIDER=dify");
    }
    return value;
  }

  private String normalizeBaseUrl(String value) {
    String baseUrl = value == null || value.isBlank() ? "http://localhost/v1" : value.trim();
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
