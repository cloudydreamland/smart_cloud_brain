package com.smartcloudbrain.ai.provider.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockAiProviderTest {

  private final MockAiProvider provider = new MockAiProvider();

  @Test
  void triageRecommendsCardiologyForChestPain() {
    var response = provider.triage(new TriageRequest(1L, "胸痛伴气短"));

    assertEquals("CARDIOLOGY", response.departmentCode());
    assertFalse(response.degraded());
  }

  @Test
  void prescriptionCheckFlagsAspirinRisk() {
    var response = provider.checkPrescription(new PrescriptionCheckRequest(
        1L,
        1L,
        List.of(new DrugItem("阿司匹林", "100mg", "每日一次", "口服"))
    ));

    assertEquals("MEDIUM", response.riskLevel());
    assertTrue(response.suggestions().contains("bleeding"));
  }
}
