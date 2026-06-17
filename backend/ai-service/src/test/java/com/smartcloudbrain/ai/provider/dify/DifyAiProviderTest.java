package com.smartcloudbrain.ai.provider.dify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.config.AiProviderProperties;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DifyAiProviderTest {

  private final DifyAiProvider provider = new DifyAiProvider(
      new AiProviderProperties(
          "dify",
          "",
          "",
          8000,
          new AiProviderProperties.Dify("http://localhost/v1", "triage-key", "record-key", "prescription-key")
      ),
      new ObjectMapper(),
      RestClient.builder()
  );

  @Test
  void parsesTriageOutputsFromDifyResponse() {
    var outputs = provider.parseOutputs("""
        {"data":{"outputs":{"recommendedDepartment":"心内科","departmentCode":"CARDIOLOGY","recommendedDoctorIds":[1,2],"reason":"胸痛需要心内科评估","degraded":false}}}
        """);

    assertEquals("CARDIOLOGY", outputs.get("departmentCode").asText());
    assertFalse(outputs.get("degraded").asBoolean());
  }

  @Test
  void parsesJsonStringResultFromDifyResponse() {
    var outputs = provider.parseOutputs("""
        {"data":{"outputs":{"result":"{\\"riskLevel\\":\\"MEDIUM\\",\\"suggestions\\":\\"注意出血风险\\",\\"interactions\\":[\\"阿司匹林风险\\"],\\"degraded\\":false}"}}}
        """);

    assertEquals("MEDIUM", outputs.get("riskLevel").asText());
    assertEquals("阿司匹林风险", outputs.get("interactions").get(0).asText());
  }

  @Test
  void fallsBackWhenDifyIsUnavailable() {
    var triage = provider.triage(new TriageRequest(1L, "胸痛、气短两天"));
    var record = provider.generateMedicalRecord(new MedicalRecordGenerateRequest(1L, "CARDIOLOGY", "demo"));
    var prescription = provider.checkPrescription(new PrescriptionCheckRequest(
        1L,
        1L,
        List.of(new DrugItem("阿司匹林", "100mg", "每日一次", "口服"))
    ));

    assertEquals("CARDIOLOGY", triage.departmentCode());
    assertEquals("Chest pain with dyspnea for two days", record.chiefComplaint());
    assertEquals("MEDIUM", prescription.riskLevel());
  }
}
