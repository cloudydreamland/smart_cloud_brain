package com.smartcloudbrain.ai.provider.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.ExistingSchedule;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockAiProviderTest {

  private final MockAiProvider provider = new MockAiProvider();

  @Test
  void triageRecommendsCardiologyForChestPain() {
    var response = provider.triage(new TriageRequest(1L, "chest pain with shortness of breath"));

    assertEquals("CARDIOLOGY", response.departmentCode());
    assertTrue(response.degraded());
  }

  @Test
  void prescriptionCheckFlagsAspirinRisk() {
    var response = provider.checkPrescription(new PrescriptionCheckRequest(
        1L,
        1L,
        List.of(new DrugItem("aspirin", "100mg", "once daily", "oral", 7, ""))
    ));

    assertEquals("MEDIUM", response.riskLevel());
    assertTrue(response.suggestions().contains("出血"));
  }

  @Test
  void generatesDegradedMedicalRecordAndNonConflictingSchedule() {
    assertTrue(provider.generateMedicalRecord(
        new MedicalRecordGenerateRequest(1L, "CARDIOLOGY", "胸痛")).degraded());
    var response = provider.suggestSchedule(new ScheduleSuggestRequest(
        LocalDate.of(2026, 6, 21), 2,
        List.of(
            new ScheduleDoctorCandidate(1L, "医生", 1L, "CARDIOLOGY", "胸痛", true),
            new ScheduleDoctorCandidate(2L, "停用医生", 1L, "CARDIOLOGY", "胸痛", false)),
        List.of(),
        List.of(new ExistingSchedule(1L, LocalDate.of(2026, 6, 21), "09:00-12:00"))));

    assertTrue(response.degraded());
    assertEquals(1, response.suggestions().size());
    assertEquals("14:00-17:00", response.suggestions().get(0).timeRange());
  }
}
