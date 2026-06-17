package com.smartcloudbrain.notification.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.entity.NotificationMessage;
import com.smartcloudbrain.notification.repository.NotificationMessageRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.notification.websocket.NotificationWebSocketHandler;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationMessageRepository notificationRepository;
  private final CurrentUserService currentUserService;
  private final NotificationWebSocketHandler webSocketHandler;

  public NotificationService(
      NotificationMessageRepository notificationRepository,
      CurrentUserService currentUserService,
      NotificationWebSocketHandler webSocketHandler
  ) {
    this.notificationRepository = notificationRepository;
    this.currentUserService = currentUserService;
    this.webSocketHandler = webSocketHandler;
  }

  public List<Map<String, Object>> list(String readStatus) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    List<NotificationMessage> messages = readStatus == null || readStatus.isBlank()
        ? notificationRepository.findByDoctorId(user.userId())
        : notificationRepository.findByDoctorIdAndReadStatus(user.userId(), readStatus);
    return messages.stream().map(this::notificationView).toList();
  }

  @Transactional
  public Map<String, Object> markRead(Long notificationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    NotificationMessage message = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!message.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    message.setReadStatus("READ");
    return notificationView(notificationRepository.save(message));
  }

  @Transactional
  public Map<String, Object> create(NotificationCreateRequest request) {
    NotificationMessage message = new NotificationMessage();
    message.setDoctorId(request.doctorId());
    message.setPatientId(request.patientId());
    message.setPrescriptionId(request.prescriptionId());
    message.setType(request.type());
    message.setTitle(request.title());
    message.setContent(request.content());
    message.setRiskLevel(request.riskLevel());
    message.setReadStatus("UNREAD");
    NotificationMessage saved = notificationRepository.save(message);
    webSocketHandler.sendToDoctor(request.doctorId(), """
        {"type":"%s","notificationId":%d,"riskLevel":"%s","title":"%s","content":"%s"}
        """.formatted(
        saved.getType(),
        saved.getId(),
        saved.getRiskLevel() == null ? "" : saved.getRiskLevel(),
        escape(saved.getTitle()),
        escape(saved.getContent())
    ).trim());
    return notificationView(saved);
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

  private String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}


