package com.smartcloudbrain.ai.provider.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.config.AiProviderProperties;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleProviderTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) server.stop(0);
  }

  @Test
  void runsAllFourOpenAiCompatibleTasks() throws Exception {
    List<String> contents = List.of(
        "{\"recommendedDepartment\":\"心内科\",\"departmentCode\":\"CARDIOLOGY\",\"recommendedDoctorDirection\":\"胸痛门诊\",\"urgencyLevel\":\"URGENT\",\"confidence\":0.9,\"recommendedDoctorIds\":[1],\"reason\":\"胸痛\"}",
        "{\"chiefComplaint\":\"胸痛\",\"presentIllness\":\"两天\",\"pastHistory\":\"无\",\"physicalExam\":\"正常\",\"diagnosis\":\"待查\",\"treatmentAdvice\":\"复查\",\"soapContent\":\"SOAP\"}",
        "{\"riskLevel\":\"LOW\",\"riskDescription\":\"低风险\",\"suggestions\":\"可用\",\"interactions\":[],\"contraindications\":[],\"adjustmentSuggestions\":[]}",
        "{\"suggestions\":[{\"doctorId\":1,\"departmentId\":1,\"workDate\":\"2026-06-21\",\"timeRange\":\"09:00-12:00\",\"capacity\":12,\"reason\":\"需求\"}]}"
    );
    AtomicInteger index = new AtomicInteger();
    ObjectMapper mapper = new ObjectMapper();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/v1/chat/completions", exchange -> {
      String body = mapper.writeValueAsString(Map.of(
          "choices", List.of(Map.of("message", Map.of("content", contents.get(index.getAndIncrement()))))));
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getRequestBody().readAllBytes();
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();

    OpenAiCompatibleProvider provider = new OpenAiCompatibleProvider(properties(server.getAddress().getPort()), mapper);
    PromptResolveResponse prompt = new PromptResolveResponse("GENERAL", "GENERAL", "template", "prompt", "{}", "v1");
    assertEquals("CARDIOLOGY", provider.triage(new TriageRequest(1L, "胸痛"), prompt).departmentCode());
    assertEquals("待查", provider.generateMedicalRecord(
        new MedicalRecordGenerateRequest(1L, "CARDIOLOGY", "胸痛两天"), prompt).diagnosis());
    assertEquals("LOW", provider.checkPrescription(new PrescriptionCheckRequest(
        1L, 1L, List.of(new DrugItem("药品", "1片", "每日一次", "口服", 3, ""))), prompt).riskLevel());
    var schedule = provider.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 1,
        List.of(new ScheduleDoctorCandidate(1L, "医生", 1L, "CARDIOLOGY", "胸痛", true)),
        List.of(), List.of()), prompt);
    assertEquals(12, schedule.suggestions().get(0).capacity());
    assertFalse(schedule.degraded());
  }

  private AiProviderProperties properties(int port) {
    return new AiProviderProperties(
        "openai", 3000,
        new AiProviderProperties.OpenAi("http://127.0.0.1:" + port + "/v1", "test-key", "deepseek-chat"),
        null, null, null, null, null);
  }
}
