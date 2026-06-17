package com.smartcloudbrain.ai.provider.dify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.config.AiProviderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DifyAiProviderTest {

  private final DifyAiProvider provider = new DifyAiProvider(
      new AiProviderProperties(
          "dify",
          8000,
          null,
          new AiProviderProperties.Dify("http://localhost/v1", "dify-key")
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
}
