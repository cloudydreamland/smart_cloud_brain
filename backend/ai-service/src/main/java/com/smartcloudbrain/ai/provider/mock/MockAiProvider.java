package com.smartcloudbrain.ai.provider.mock;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.util.List;
import java.util.ArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "mock")
public class MockAiProvider implements AiProvider {

  @Override
  public String providerName() {
    return "mock";
  }

  @Override
  public TriageResponse triage(TriageRequest request, PromptResolveResponse prompt) {
    boolean cardiology = request.chiefComplaint().contains("胸痛")
        || request.chiefComplaint().toLowerCase().contains("chest");
    if (cardiology) {
      return new TriageResponse("Cardiology", "CARDIOLOGY", List.of(1L), "Deterministic fallback: complaint suggests cardiology assessment.", true);
    }
    return new TriageResponse("General Practice", "GENERAL", List.of(), "Fallback to manual triage.", true);
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request, PromptResolveResponse prompt) {
    return new MedicalRecordGenerateResponse(
        "Chest pain with dyspnea for two days",
        "Symptoms worsen after activity and are relieved by rest.",
        "No clear past history was provided.",
        "Physical examination should be completed by the doctor.",
        "Chest pain under evaluation.",
        "Complete ECG and cardiac enzyme checks.",
        true
    );
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request, PromptResolveResponse prompt) {
    boolean aspirin = request.drugs().stream()
        .map(DrugItem::drugName)
        .anyMatch(name -> name.contains("阿司匹林") || name.toLowerCase().contains("aspirin"));
    if (aspirin) {
      return new PrescriptionCheckResponse("MEDIUM", "Deterministic fallback: review bleeding risk before saving.", List.of("Aspirin may increase bleeding risk."), true);
    }
    return new PrescriptionCheckResponse("LOW", "Deterministic fallback found no obvious high-risk interaction.", List.of(), true);
  }

  @Override
  public ScheduleSuggestResponse suggestSchedule(ScheduleSuggestRequest request, PromptResolveResponse prompt) {
    List<ScheduleSuggestionItem> suggestions = new ArrayList<>();
    for (ScheduleDoctorCandidate doctor : request.doctors()) {
      if (!doctor.enabled()) {
        continue;
      }
      for (int offset = 0; offset < request.days(); offset++) {
        var workDate = request.startDate().plusDays(offset);
        String timeRange = offset % 2 == 0 ? "09:00-12:00" : "14:00-17:00";
        boolean occupied = request.existingSchedules().stream().anyMatch(existing ->
            doctor.doctorId().equals(existing.doctorId())
                && workDate.equals(existing.workDate())
                && timeRange.equals(existing.timeRange()));
        if (!occupied) {
          suggestions.add(new ScheduleSuggestionItem(
              doctor.doctorId(),
              doctor.departmentId(),
              workDate,
              timeRange,
              12,
              "确定性规则降级：按启用医生、科室和日期轮换上午/下午门诊"
          ));
        }
      }
    }
    return new ScheduleSuggestResponse(suggestions, "mock", true);
  }
}
