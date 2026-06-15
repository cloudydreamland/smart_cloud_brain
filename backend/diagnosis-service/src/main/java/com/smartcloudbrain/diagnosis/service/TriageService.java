package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.diagnosis.entity.TriageRecord;
import com.smartcloudbrain.diagnosis.repository.TriageRecordRepository;
import com.smartcloudbrain.diagnosis.security.CurrentUser;
import com.smartcloudbrain.diagnosis.security.CurrentUserService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TriageService {

  private final AiGatewayService aiGatewayService;
  private final TriageRecordRepository triageRecordRepository;
  private final CurrentUserService currentUserService;

  public TriageService(
      AiGatewayService aiGatewayService,
      TriageRecordRepository triageRecordRepository,
      CurrentUserService currentUserService
  ) {
    this.aiGatewayService = aiGatewayService;
    this.triageRecordRepository = triageRecordRepository;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public Map<String, Object> consult(TriageRequest request) {
    CurrentUser currentUser = currentUserService.get();
    Long patientId = request.patientId() == null ? currentUser.userId() : request.patientId();
    TriageResponse response = aiGatewayService.triage(new TriageRequest(patientId, request.chiefComplaint()));
    TriageRecord record = new TriageRecord();
    record.setPatientId(patientId);
    record.setChiefComplaint(request.chiefComplaint());
    record.setRecommendedDepartment(response.recommendedDepartment());
    record.setRecommendedDoctorIds(response.recommendedDoctorIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
    record.setReason(response.reason());
    record.setAiResultJson("""
        {"departmentCode":"%s","degraded":%s}
        """.formatted(response.departmentCode(), response.degraded()).trim());
    record.setStatus(response.degraded() ? "MANUAL_REQUIRED" : "AI_RECOMMENDED");
    return triageView(triageRecordRepository.save(record), response);
  }

  public List<Map<String, Object>> list() {
    CurrentUser currentUser = currentUserService.get();
    List<TriageRecord> records = currentUser.role() == RoleType.PATIENT
        ? triageRecordRepository.findByPatientId(currentUser.userId())
        : triageRecordRepository.findAll();
    return records.stream().map(record -> triageView(record, null)).toList();
  }

  private Map<String, Object> triageView(TriageRecord record, TriageResponse response) {
    return Map.of(
        "triageRecordId", record.getId(),
        "patientId", record.getPatientId(),
        "chiefComplaint", record.getChiefComplaint(),
        "recommendedDepartment", record.getRecommendedDepartment() == null ? "" : record.getRecommendedDepartment(),
        "departmentCode", response == null ? "" : response.departmentCode(),
        "recommendedDoctorIds", response == null ? record.getRecommendedDoctorIds() : response.recommendedDoctorIds(),
        "reason", record.getReason() == null ? "" : record.getReason(),
        "status", record.getStatus(),
        "degraded", response != null && response.degraded()
    );
  }
}
