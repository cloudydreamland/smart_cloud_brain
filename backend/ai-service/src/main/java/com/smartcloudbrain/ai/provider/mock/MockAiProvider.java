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
  public String modelName() {
    return "local-demo";
  }

  @Override
  public TriageResponse triage(TriageRequest request, PromptResolveResponse prompt) {
    boolean cardiology = request.chiefComplaint().contains("胸痛")
        || request.chiefComplaint().toLowerCase().contains("chest");
    if (cardiology) {
      return new TriageResponse("心内科", "CARDIOLOGY", List.of(1L), "确定性降级：主诉提示心内科评估。", true);
    }
    return new TriageResponse("全科", "GENERAL", List.of(), "降级为人工分诊。", true);
  }

  @Override
  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request, PromptResolveResponse prompt) {
    return new MedicalRecordGenerateResponse(
        "胸痛伴呼吸困难两天",
        "活动后加重，休息后缓解。",
        "未提供明确既往病史。",
        "体格检查应由医生完成。",
        "胸痛待查。",
        "完善心电图和心肌酶检查。",
        true
    );
  }

  @Override
  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request, PromptResolveResponse prompt) {
    boolean aspirin = request.drugs().stream()
        .map(DrugItem::drugName)
        .anyMatch(name -> name.contains("阿司匹林") || name.toLowerCase().contains("aspirin"));
    if (aspirin) {
      return new PrescriptionCheckResponse("MEDIUM", "确定性降级：保存前请审核出血风险。", List.of("阿司匹林可能增加出血风险。"), true);
    }
    return new PrescriptionCheckResponse("LOW", "确定性降级：未发现明显高风险药物相互作用。", List.of(), true);
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
