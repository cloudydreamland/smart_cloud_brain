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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

  public List<Map<String, Object>> list(
      String readStatus,
      String handleStatus,
      String type,
      String riskLevel,
      String q,
      String sort
  ) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    return notificationRepository.findByDoctorId(user.userId()).stream()
        .filter(message -> matches(message.getReadStatus(), readStatus))
        .filter(message -> matches(message.getHandleStatus(), handleStatus))
        .filter(message -> matches(message.getType(), type))
        .filter(message -> matches(message.getRiskLevel(), riskLevel))
        .filter(message -> matchesSearch(message, q))
        .sorted(sortComparator(sort))
        .map(this::notificationView)
        .toList();
  }

  @Transactional
  public Map<String, Object> markRead(Long notificationId) {
    NotificationMessage message = currentDoctorMessage(notificationId);
    message.setReadStatus("READ");
    return notificationView(notificationRepository.save(message));
  }

  @Transactional
  public Map<String, Object> handle(Long notificationId, String handleStatus) {
    NotificationMessage message = currentDoctorMessage(notificationId);
    String status = normalize(handleStatus);
    if (!List.of("HANDLED", "IGNORED").contains(status)) {
      throw new BusinessException(400, "Unsupported notification handle status");
    }
    message.setReadStatus("READ");
    message.setHandleStatus(status);
    message.setHandledAt(LocalDateTime.now());
    return notificationView(notificationRepository.save(message));
  }

  @Transactional
  public Map<String, Object> create(NotificationCreateRequest request) {
    String type = normalize(defaultText(request.type(), "SYSTEM_NOTICE"));
    if ("TRIAGE_ASSIGN".equalsIgnoreCase(type) && request.triageRecordId() != null) {
      var existing = notificationRepository.findFirstByDoctorIdAndTriageRecordIdAndType(
          request.doctorId(),
          request.triageRecordId(),
          "TRIAGE_ASSIGN"
      );
      if (existing.isPresent()) {
        return notificationView(existing.get());
      }
    }
    NotificationMessage message = new NotificationMessage();
    message.setDoctorId(request.doctorId());
    message.setPatientId(request.patientId());
    message.setPrescriptionId(request.prescriptionId());
    message.setTriageRecordId(request.triageRecordId());
    message.setMedicalRecordId(request.medicalRecordId());
    message.setType(type);
    message.setTitle(defaultText(request.title(), "系统通知"));
    message.setContent(defaultText(request.content(), "请查看通知详情。"));
    message.setRiskLevel(request.riskLevel());
    message.setReadStatus("UNREAD");
    message.setHandleStatus("PENDING");
    NotificationMessage saved = notificationRepository.save(message);
    webSocketHandler.sendToDoctor(request.doctorId(), """
        {"type":"%s","notificationId":%d,"riskLevel":"%s","handleStatus":"%s","title":"%s","content":"%s"}
        """.formatted(
        saved.getType(),
        saved.getId(),
        saved.getRiskLevel() == null ? "" : saved.getRiskLevel(),
        saved.getHandleStatus() == null ? "" : saved.getHandleStatus(),
        escape(saved.getTitle()),
        escape(saved.getContent())
    ).trim());
    return notificationView(saved);
  }

  private NotificationMessage currentDoctorMessage(Long notificationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    NotificationMessage message = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!message.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return message;
  }

  private boolean matches(String actual, String expected) {
    return expected == null || expected.isBlank() || normalize(actual).equals(normalize(expected));
  }

  private boolean matchesSearch(NotificationMessage message, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String value = query.toLowerCase(Locale.ROOT).trim();
    return text(message.getTitle()).contains(value)
        || text(message.getContent()).contains(value)
        || text(message.getType()).contains(value)
        || String.valueOf(message.getPatientId()).contains(value)
        || String.valueOf(message.getPrescriptionId()).contains(value)
        || String.valueOf(message.getTriageRecordId()).contains(value)
        || String.valueOf(message.getMedicalRecordId()).contains(value);
  }

  private Comparator<NotificationMessage> sortComparator(String sort) {
    String value = normalize(sort);
    return switch (value) {
      case "CREATED_DESC" -> Comparator
          .comparingLong(this::createdDesc)
          .thenComparing(message -> message.getId() == null ? 0L : -message.getId());
      case "CREATED_ASC" -> Comparator.<NotificationMessage, LocalDateTime>comparing(
              NotificationMessage::getCreatedAt,
              Comparator.nullsLast(Comparator.naturalOrder())
          )
          .thenComparing(message -> message.getId() == null ? 0L : message.getId());
      case "RISK_DESC" -> Comparator
          .comparingInt((NotificationMessage message) -> riskPriority(message))
          .thenComparingLong(this::createdDesc);
      case "UNREAD_FIRST" -> Comparator
          .comparingInt((NotificationMessage message) -> "READ".equals(normalize(message.getReadStatus())) ? 1 : 0)
          .thenComparingInt(this::handlePriority)
          .thenComparingLong(this::createdDesc);
      case "HANDLED_FIRST" -> Comparator
          .comparingInt((NotificationMessage message) -> "HANDLED".equals(normalize(message.getHandleStatus())) ? 0 : 1)
          .thenComparingLong(this::createdDesc);
      default -> Comparator
          .comparingInt((NotificationMessage message) -> handlePriority(message))
          .thenComparingInt(this::riskPriority)
          .thenComparingLong(this::createdDesc);
    };
  }

  private long createdDesc(NotificationMessage message) {
    LocalDateTime createdAt = message.getCreatedAt();
    return createdAt == null ? Long.MAX_VALUE : -createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  private int handlePriority(NotificationMessage message) {
    return switch (normalize(message.getHandleStatus())) {
      case "PENDING" -> 0;
      case "HANDLED" -> 1;
      case "IGNORED" -> 2;
      default -> 3;
    };
  }

  private int riskPriority(NotificationMessage message) {
    return switch (normalize(message.getRiskLevel())) {
      case "HIGH" -> 0;
      case "MEDIUM" -> 1;
      case "LOW" -> 2;
      default -> 3;
    };
  }

  private Map<String, Object> notificationView(NotificationMessage message) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("notificationId", message.getId());
    view.put("doctorId", message.getDoctorId());
    view.put("patientId", message.getPatientId() == null ? 0L : message.getPatientId());
    view.put("prescriptionId", message.getPrescriptionId() == null ? 0L : message.getPrescriptionId());
    view.put("triageRecordId", message.getTriageRecordId() == null ? 0L : message.getTriageRecordId());
    view.put("medicalRecordId", message.getMedicalRecordId() == null ? 0L : message.getMedicalRecordId());
    view.put("type", message.getType());
    view.put("title", message.getTitle());
    view.put("content", message.getContent());
    view.put("riskLevel", message.getRiskLevel() == null ? "" : message.getRiskLevel());
    view.put("readStatus", message.getReadStatus());
    view.put("handleStatus", message.getHandleStatus() == null ? "PENDING" : message.getHandleStatus());
    view.put("createdAt", message.getCreatedAt() == null ? "" : message.getCreatedAt().toString());
    view.put("handledAt", message.getHandledAt() == null ? "" : message.getHandledAt().toString());
    return view;
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private String text(Object value) {
    return String.valueOf(value == null ? "" : value).toLowerCase(Locale.ROOT);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
