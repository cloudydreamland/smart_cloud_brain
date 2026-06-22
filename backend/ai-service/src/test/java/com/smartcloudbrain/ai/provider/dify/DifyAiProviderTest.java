package com.smartcloudbrain.ai.provider.dify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.smartcloudbrain.ai.config.AiProviderProperties;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.ExistingSchedule;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleDepartmentCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import java.time.LocalDate;
import java.util.List;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DifyAiProviderTest {

  private final DifyAiProvider provider = new DifyAiProvider(
      new AiProviderProperties(
          "dify",
          8000,
          null,
          new AiProviderProperties.Dify("http://localhost/v1", "legacy-key"),
          new AiProviderProperties.DifyWorkflow("triage-key"),
          new AiProviderProperties.DifyWorkflow("medical-key"),
          new AiProviderProperties.DifyWorkflow("prescription-key"),
          new AiProviderProperties.DifyWorkflow("schedule-key")
      ),
      new ObjectMapper(),
      RestClient.builder()
  );

  @Test
  void parsesTriageOutputsFromDifyResponse() {
    var outputs = provider.parseOutputs("""
        {"data":{"outputs":{"recommendedDepartment":"Cardiology","departmentCode":"CARDIOLOGY","recommendedDoctorDirection":"cardiology outpatient","urgencyLevel":"URGENT","confidence":0.91,"recommendedDoctorIds":[1,2],"reason":"chest pain needs cardiology review","degraded":false}}}
        """);

    assertEquals("CARDIOLOGY", outputs.get("departmentCode").asText());
    assertFalse(outputs.get("degraded").asBoolean());
  }

  @Test
  void parsesJsonStringResultFromDifyResponse() {
    var outputs = provider.parseOutputs("""
        {"data":{"outputs":{"result":"{\\"riskLevel\\":\\"MEDIUM\\",\\"suggestions\\":\\"review bleeding risk\\",\\"interactions\\":[\\"aspirin risk\\"],\\"degraded\\":false}"}}}
        """);

    assertEquals("MEDIUM", outputs.get("riskLevel").asText());
    assertEquals("aspirin risk", outputs.get("interactions").get(0).asText());
  }

  @Test
  void parsesLegacyTriageOutputWithoutEnrichmentFields() {
    var outputs = provider.parseOutputs("""
        {"data":{"outputs":{"result":"{\\"recommendedDepartment\\":\\"Cardiology\\",\\"departmentCode\\":\\"CARDIOLOGY\\",\\"recommendedDoctorIds\\":[],\\"reason\\":\\"chest pain\\",\\"degraded\\":false}"}}}
        """);

    assertEquals("Cardiology", outputs.get("recommendedDepartment").asText());
    assertFalse(outputs.has("recommendedDoctorDirection"));
    assertFalse(outputs.has("urgencyLevel"));
    assertFalse(outputs.has("confidence"));
  }

  @Test
  void selectsTaskSpecificApiKeys() {
    assertEquals("triage-key", provider.apiKeyFor(DifyAiProvider.WorkflowTask.TRIAGE));
    assertEquals("medical-key", provider.apiKeyFor(DifyAiProvider.WorkflowTask.MEDICAL_RECORD));
    assertEquals("prescription-key", provider.apiKeyFor(DifyAiProvider.WorkflowTask.PRESCRIPTION_CHECK));
    assertEquals("schedule-key", provider.apiKeyFor(DifyAiProvider.WorkflowTask.SCHEDULE));
  }

  @Test
  void fallsBackToDeprecatedSharedApiKey() {
    DifyAiProvider fallbackProvider = providerWithKeys("legacy-key", "", "", "", "");

    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.TRIAGE));
    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.MEDICAL_RECORD));
    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.PRESCRIPTION_CHECK));
    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.SCHEDULE));
  }

  @Test
  void rejectsMissingTaskAndFallbackKey() {
    DifyAiProvider missingKeyProvider = providerWithKeys("", "triage-key", "medical-key", "", "schedule-key");

    assertThrows(
        IllegalStateException.class,
        () -> missingKeyProvider.apiKeyFor(DifyAiProvider.WorkflowTask.PRESCRIPTION_CHECK)
    );
  }

  @Test
  void runsAndParsesAllFourDifyWorkflows() throws Exception {
    List<String> responses = List.of(
        "{\"data\":{\"outputs\":{\"recommendedDepartment\":\"心内科\",\"departmentCode\":\"CARDIOLOGY\",\"recommendedDoctorDirection\":\"胸痛门诊\",\"urgencyLevel\":\"URGENT\",\"confidence\":0.9,\"recommendedDoctorIds\":[1],\"reason\":\"胸痛\"}}}",
        "{\"data\":{\"outputs\":{\"chiefComplaint\":\"胸痛\",\"presentIllness\":\"两天\",\"pastHistory\":\"无\",\"physicalExam\":\"正常\",\"diagnosis\":\"待查\",\"treatmentAdvice\":\"心电图\",\"soapContent\":\"SOAP\"}}}",
        "{\"data\":{\"outputs\":{\"riskLevel\":\"MEDIUM\",\"riskDescription\":\"出血风险\",\"suggestions\":\"复核\",\"interactions\":\"阿司匹林风险\"}}}",
        "{\"data\":{\"outputs\":{\"suggestions\":[{\"doctorId\":2,\"departmentId\":3,\"workDate\":\"2026-06-21\",\"timeRange\":\"09:00-12:00\",\"capacity\":12,\"reason\":\"需求较高\"}]}}}",
        "{\"data\":{\"outputs\":{}}}",
        "{\"data\":{\"outputs\":{\"suggestions\":[{\"doctorId\":2,\"departmentId\":3,\"workDate\":\"2026-06-21\",\"timeRange\":\"14:00-17:00\",\"capacity\":10,\"reason\":\"纠错成功\"}]}}}",
        "{\"data\":{\"outputs\":{}}}",
        "{\"data\":{\"outputs\":{}}}"
    );
    AtomicInteger index = new AtomicInteger();
    List<String> requestBodies = new CopyOnWriteArrayList<>();
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext("/v1/workflows/run", exchange -> {
      byte[] bytes = responses.get(index.getAndIncrement()).getBytes(StandardCharsets.UTF_8);
      requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    httpServer.start();
    DifyAiProvider liveProvider = providerWithBaseUrl("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/v1");
    PromptResolveResponse prompt = new PromptResolveResponse(1L, "GENERAL", "GENERAL", "template", "prompt", "{}", "v1");

    assertEquals("CARDIOLOGY", liveProvider.triage(new TriageRequest(1L, "胸痛"), prompt).departmentCode());

    assertEquals("待查", liveProvider.generateMedicalRecord(
        new MedicalRecordGenerateRequest(1L, "CARDIOLOGY", "胸痛两天"), prompt).diagnosis());

    assertEquals(1, liveProvider.checkPrescription(new PrescriptionCheckRequest(
        1L, 2L, List.of(new DrugItem("阿司匹林", "100mg", "每日一次", "口服", 3, ""))), prompt).interactions().size());

    var schedule = liveProvider.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1,
        List.of(new ScheduleDoctorCandidate(2L, "医生", 3L, "CARDIOLOGY", "胸痛", true)),
        List.of(new ScheduleDepartmentCandidate(3L, "CARDIOLOGY", "心内科")),
        List.of(new ExistingSchedule(2L, LocalDate.of(2026, 6, 20), "09:00-12:00"))
    ), prompt);
    assertEquals("dify", schedule.provider());
    assertEquals(12, schedule.suggestions().get(0).capacity());

    var corrected = liveProvider.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1,
        List.of(new ScheduleDoctorCandidate(2L, "医生", 3L, "CARDIOLOGY", "胸痛", true)),
        List.of(new ScheduleDepartmentCandidate(3L, "CARDIOLOGY", "心内科")),
        List.of()), prompt);
    assertEquals(10, corrected.suggestions().get(0).capacity());
    assertTrue(requestBodies.get(5).contains("validationFeedback"));

    assertThrows(IllegalStateException.class, () -> liveProvider.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1,
        List.of(new ScheduleDoctorCandidate(2L, "医生", 3L, "CARDIOLOGY", "胸痛", true)),
        List.of(), List.of()), prompt));
    httpServer.stop(0);
  }

  @Test
  void rejectsMalformedDifyOutputAndMissingPrompt() {
    assertEquals("dify", provider.providerName());
    assertEquals("dify-workflow", provider.modelName());
    assertThrows(IllegalArgumentException.class, () -> provider.parseOutputs("not-json"));
    assertThrows(IllegalArgumentException.class, () -> provider.parseOutputs("{\"data\":{}}"));
    assertEquals(1, provider.parseOutputs("{\"data\":{\"outputs\":\"{\\\"x\\\":1}\"}}").get("x").asInt());
    assertThrows(IllegalStateException.class, () -> provider.triage(new TriageRequest(1L, "胸痛"), null));
  }

  @Test
  void coversDefensiveParserAndSerializationFailures() throws Exception {
    JsonNode empty = new ObjectMapper().readTree("{}");
    assertInvocationCause("requiredText", new Class<?>[]{JsonNode.class, String.class},
        new Object[]{empty, "missing"}, IllegalStateException.class);
    assertInvocationCause("requiredLong", new Class<?>[]{JsonNode.class, String.class},
        new Object[]{empty, "doctorId"}, IllegalStateException.class);
    assertInvocationCause("requiredInt", new Class<?>[]{JsonNode.class, String.class},
        new Object[]{empty, "capacity"}, IllegalStateException.class);
    Method normalizeRiskLevel = DifyAiProvider.class.getDeclaredMethod("normalizeRiskLevel", String.class);
    normalizeRiskLevel.setAccessible(true);
    assertEquals("LOW", normalizeRiskLevel.invoke(provider, "低风险"));
    assertEquals("MEDIUM", normalizeRiskLevel.invoke(provider, "中"));
    assertEquals("HIGH", normalizeRiskLevel.invoke(provider, "HIGH"));
    InvocationTargetException unsupportedRisk = assertThrows(InvocationTargetException.class,
        () -> normalizeRiskLevel.invoke(provider, "unknown"));
    assertEquals(IllegalStateException.class, unsupportedRisk.getCause().getClass());
    Method drugTextInput = DifyAiProvider.class.getDeclaredMethod("drugTextInput", List.class);
    drugTextInput.setAccessible(true);
    assertEquals("", drugTextInput.invoke(provider, List.of()));
    Method stringList = DifyAiProvider.class.getDeclaredMethod("stringList", JsonNode.class);
    stringList.setAccessible(true);
    assertEquals(List.of("x"), stringList.invoke(provider, new ObjectMapper().readTree("[\"x\"]")));
    Method longList = DifyAiProvider.class.getDeclaredMethod("longList", JsonNode.class);
    longList.setAccessible(true);
    assertEquals(List.of(), longList.invoke(provider, new Object[]{null}));
    assertEquals(List.of(7L), longList.invoke(provider, new ObjectMapper().readTree("7")));

    ObjectMapper failingMapper = mock(ObjectMapper.class);
    when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("serialization failed") { });
    DifyAiProvider failing = new DifyAiProvider(
        new AiProviderProperties("dify", 8000, null,
            new AiProviderProperties.Dify("http://localhost/v1", "legacy"),
            null, null, null, null), failingMapper, RestClient.builder());
    Method jsonInput = DifyAiProvider.class.getDeclaredMethod("jsonInput", Object.class);
    jsonInput.setAccessible(true);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
        () -> jsonInput.invoke(failing, List.of("x")));
    assertEquals(IllegalArgumentException.class, thrown.getCause().getClass());

    assertThrows(IllegalStateException.class, () -> new DifyAiProvider(
        new AiProviderProperties("dify", 8000, null, null, null, null, null, null),
        new ObjectMapper(), RestClient.builder()));
  }

  private void assertInvocationCause(String name, Class<?>[] parameterTypes, Object[] args, Class<?> causeType) throws Exception {
    Method method = DifyAiProvider.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> method.invoke(provider, args));
    assertEquals(causeType, thrown.getCause().getClass());
  }

  private DifyAiProvider providerWithBaseUrl(String baseUrl) {
    return new DifyAiProvider(
        new AiProviderProperties(
            "dify", 8000, null,
            new AiProviderProperties.Dify(baseUrl, "legacy-key"),
            new AiProviderProperties.DifyWorkflow("triage-key"),
            new AiProviderProperties.DifyWorkflow("medical-key"),
            new AiProviderProperties.DifyWorkflow("prescription-key"),
            new AiProviderProperties.DifyWorkflow("schedule-key")
        ), new ObjectMapper().findAndRegisterModules(), RestClient.builder());
  }

  private DifyAiProvider providerWithKeys(String legacy, String triage, String medical, String prescription, String schedule) {
    return new DifyAiProvider(
        new AiProviderProperties(
            "dify",
            8000,
            null,
            new AiProviderProperties.Dify("http://localhost/v1", legacy),
            new AiProviderProperties.DifyWorkflow(triage),
            new AiProviderProperties.DifyWorkflow(medical),
            new AiProviderProperties.DifyWorkflow(prescription),
            new AiProviderProperties.DifyWorkflow(schedule)
        ),
        new ObjectMapper(),
        RestClient.builder()
    );
  }
}
