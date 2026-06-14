package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.diagnosis.entity.NotificationMessage;
import com.smartcloudbrain.diagnosis.repository.NotificationMessageRepository;
import com.smartcloudbrain.diagnosis.security.CurrentUser;
import com.smartcloudbrain.diagnosis.security.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationMessageRepository notificationRepository;
  private final CurrentUserService currentUserService;

  public NotificationService(NotificationMessageRepository notificationRepository, CurrentUserService currentUserService) {
    this.notificationRepository = notificationRepository;
    this.currentUserService = currentUserService;
  }

  public List<Map<String, Object>> list(String readStatus) {
    CurrentUser currentUser = currentUserService.get();
    List<NotificationMessage> messages = readStatus == null || readStatus.isBlank()
        ? notificationRepository.findByDoctorId(currentUser.userId())
        : notificationRepository.findByDoctorIdAndReadStatus(currentUser.userId(), readStatus);
    return messages.stream().map(this::notificationView).toList();
  }

  @Transactional
  public Map<String, Object> markRead(Long notificationId) {
    NotificationMessage message = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    message.setReadStatus("READ");
    return notificationView(notificationRepository.save(message));
  }

  private Map<String, Object> notificationView(NotificationMessage message) {
    return Map.of(
        "notificationId", message.getId(),
        "doctorId", message.getDoctorId(),
        "patientId", message.getPatientId() == null ? 0L : message.getPatientId(),
        "prescriptionId", message.getPrescriptionId() == null ? 0L : message.getPrescriptionId(),
        "type", message.getType(),
        "title", message.getTitle(),
        "content", message.getContent(),
        "riskLevel", message.getRiskLevel() == null ? "" : message.getRiskLevel(),
        "readStatus", message.getReadStatus(),
        "createdAt", message.getCreatedAt() == null ? "" : message.getCreatedAt().toString()
    );
  }
}
