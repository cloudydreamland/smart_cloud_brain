package com.smartcloudbrain.ai.provider.dify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
          new AiProviderProperties.Dify("http://localhost/v1", "legacy-key"),
          new AiProviderProperties.DifyWorkflow("triage-key"),
          new AiProviderProperties.DifyWorkflow("medical-key"),
          new AiProviderProperties.DifyWorkflow("prescription-key")
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
  }

  @Test
  void fallsBackToDeprecatedSharedApiKey() {
    DifyAiProvider fallbackProvider = providerWithKeys("legacy-key", "", "", "");

    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.TRIAGE));
    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.MEDICAL_RECORD));
    assertEquals("legacy-key", fallbackProvider.apiKeyFor(DifyAiProvider.WorkflowTask.PRESCRIPTION_CHECK));
  }

  @Test
  void rejectsMissingTaskAndFallbackKey() {
    DifyAiProvider missingKeyProvider = providerWithKeys("", "triage-key", "medical-key", "");

    assertThrows(
        IllegalStateException.class,
        () -> missingKeyProvider.apiKeyFor(DifyAiProvider.WorkflowTask.PRESCRIPTION_CHECK)
    );
  }

  private DifyAiProvider providerWithKeys(String legacy, String triage, String medical, String prescription) {
    return new DifyAiProvider(
        new AiProviderProperties(
            "dify",
            8000,
            null,
            new AiProviderProperties.Dify("http://localhost/v1", legacy),
            new AiProviderProperties.DifyWorkflow(triage),
            new AiProviderProperties.DifyWorkflow(medical),
            new AiProviderProperties.DifyWorkflow(prescription)
        ),
        new ObjectMapper(),
        RestClient.builder()
    );
  }
}
