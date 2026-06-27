package com.smartcloudbrain.triage.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.triage.client.InternalNotificationClient;
import com.smartcloudbrain.triage.entity.TriageRecord;
import com.smartcloudbrain.triage.repository.TriageRecordRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TriageDeskService {

  private final TriageRecordRepository triageRecordRepository;
  private final InternalNotificationClient notificationClient;

  public TriageDeskService(TriageRecordRepository triageRecordRepository, InternalNotificationClient notificationClient) {
    this.triageRecordRepository = triageRecordRepository;
    this.notificationClient = notificationClient;
  }

  public List<Map<String, Object>> list() {
    return triageRecordRepository.findAllByOrderByIdDesc().stream().map(this::triageView).toList();
  }

  public Map<String, Object> detail(Long id) {
    return triageView(triageRecordRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  @Transactional
  public Map<String, Object> assign(Long triageRecordId, Long doctorId) {
    TriageRecord record = triageRecordRepository.findById(triageRecordId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    record.setAssignedDoctorId(doctorId);
    record.setStatus("ASSIGNED");
    TriageRecord saved = triageRecordRepository.save(record);
    publishAssignNotification(saved, doctorId);
    return triageView(saved);
  }

  @Transactional
  public Map<String, Object> close(Long triageRecordId) {
    TriageRecord record = triageRecordRepository.findById(triageRecordId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    record.setStatus("CLOSED");
    return triageView(triageRecordRepository.save(record));
  }

  private Map<String, Object> triageView(TriageRecord record) {
    return Map.of(
        "triageRecordId", record.getId(),
        "patientId", record.getPatientId(),
        "chiefComplaint", record.getChiefComplaint(),
        "recommendedDepartment", record.getRecommendedDepartment() == null ? "" : record.getRecommendedDepartment(),
        "recommendedDoctorIds", record.getRecommendedDoctorIds() == null ? "" : record.getRecommendedDoctorIds(),
        "assignedDoctorId", record.getAssignedDoctorId() == null ? 0L : record.getAssignedDoctorId(),
        "reason", record.getReason() == null ? "" : record.getReason(),
        "status", record.getStatus(),
        "createdAt", record.getCreatedAt() == null ? "" : record.getCreatedAt().toString()
    );
  }

  private void publishAssignNotification(TriageRecord record, Long doctorId) {
    if (notificationClient == null || doctorId == null) {
      return;
    }
    String content = "患者分诊已分配给您，主诉：" + record.getChiefComplaint()
        + (record.getReason() == null || record.getReason().isBlank() ? "" : "。建议：" + record.getReason());
    try {
      notificationClient.createTriageAssign(doctorId, record.getPatientId(), record.getId(), content, "");
    } catch (BusinessException ignored) {
      // 分诊分配是主流程，通知失败不应阻断业务操作。
    }
  }
}
