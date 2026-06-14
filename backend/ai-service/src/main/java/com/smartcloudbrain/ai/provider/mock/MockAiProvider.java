package com.smartcloudbrain.ai.provider.mock;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {

  @Override
  public TriageResponse triage(TriageRequest request) {
    boolean cardiology = request.chiefComplaint().contains("胸痛")
        || request.chiefComplaint().toLowerCase().contains("chest");
    if (cardiology) {
      return new TriageResponse("Cardiology", "CARDIOLOGY", List.of(1L), "Complaint suggests cardiology assessment.", false);
    }
    return new TriageResponse("General Practice", "GENERAL", List.of(), "Fallback to manual triage.", true);
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    return new MedicalRecordGenerateResponse(
        "Chest pain with dyspnea for two days",
        "Symptoms worsen after activity and are relieved by rest.",
        "No clear past history was provided.",
        "Physical examination should be completed by the doctor.",
        "Chest pain under evaluation.",
        "Complete ECG and cardiac enzyme checks.",
        false
    );
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    boolean aspirin = request.drugs().stream()
        .map(DrugItem::drugName)
        .anyMatch(name -> name.contains("阿司匹林") || name.toLowerCase().contains("aspirin"));
    if (aspirin) {
      return new PrescriptionCheckResponse("MEDIUM", "Review bleeding risk before saving.", List.of("Aspirin may increase bleeding risk."), false);
    }
    return new PrescriptionCheckResponse("LOW", "No obvious high-risk interaction was found.", List.of(), false);
  }
}
