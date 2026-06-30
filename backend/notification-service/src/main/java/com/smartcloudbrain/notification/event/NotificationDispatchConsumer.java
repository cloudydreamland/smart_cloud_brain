package com.smartcloudbrain.notification.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.common.event.RabbitTopology;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.service.AuditService;
import com.smartcloudbrain.notification.service.NotificationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatchConsumer {

  private static final List<String> RISK_NOTIFICATION_LEVELS = List.of("HIGH", "MEDIUM");

  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;
  private final AuditService auditService;

  public NotificationDispatchConsumer(
      ObjectMapper objectMapper,
      NotificationService notificationService,
      AuditService auditService
  ) {
    this.objectMapper = objectMapper;
    this.notificationService = notificationService;
    this.auditService = auditService;
  }

  @RabbitListener(queues = RabbitTopology.NOTIFICATION_DISPATCH_QUEUE)
  public void onMessage(String message) throws Exception {
    DomainEventEnvelope envelope = objectMapper.readValue(message, DomainEventEnvelope.class);
    Map<String, Object> payload = objectMapper.convertValue(envelope.payload(), new TypeReference<>() {
    });
    NotificationCreateRequest request = toRequest(envelope.eventType(), payload);
    if (request == null || request.doctorId() == null) {
      return;
    }
    notificationService.create(request);
    auditNotificationProcessed(envelope, request);
  }

  private NotificationCreateRequest toRequest(String eventType, Map<String, Object> payload) {
    if (DomainEventNames.PRESCRIPTION_CHECKED.equals(eventType)) {
      String riskLevel = asString(payload.get("riskLevel"));
      if (!RISK_NOTIFICATION_LEVELS.contains(riskLevel)) {
        return null;
      }
      return createRequest(payload, "PRESCRIPTION_HIGH_RISK", "AI处方风险提醒", asString(payload.get("suggestions")));
    }
    if (DomainEventNames.TRIAGE_ASSIGNED.equals(eventType)) {
      return createRequest(payload, "TRIAGE_ASSIGN", "分诊分配提醒", "患者分诊已分配给您，请及时查看。");
    }
    if (DomainEventNames.REGISTRATION_CREATED.equals(eventType)) {
      return createRequest(payload, "REGISTRATION_CREATED", "挂号成功提醒", "患者已完成挂号，请关注候诊队列。");
    }
    if (DomainEventNames.REGISTRATION_CANCELLED.equals(eventType)) {
      return createRequest(payload, "REGISTRATION_CANCELLED", "挂号取消提醒", "患者已取消挂号，请留意号源变化。");
    }
    if (DomainEventNames.SCHEDULE_PUBLISHED.equals(eventType)) {
      return createRequest(payload, "SCHEDULE_PUBLISHED", "排班发布提醒", "您的门诊排班已发布。");
    }
    if (DomainEventNames.SCHEDULE_CANCELLED.equals(eventType)) {
      return createRequest(payload, "SCHEDULE_CANCELLED", "排班取消提醒", "您的门诊排班已取消。");
    }
    return null;
  }

  private NotificationCreateRequest createRequest(
      Map<String, Object> payload,
      String defaultType,
      String defaultTitle,
      String defaultContent
  ) {
    return new NotificationCreateRequest(
        asLong(payload.get("doctorId")),
        asLong(payload.get("patientId")),
        zeroToNull(asLong(payload.get("prescriptionId"))),
        defaultText(asString(payload.get("type")), defaultType),
        defaultText(asString(payload.get("title")), defaultTitle),
        defaultText(asString(payload.get("content")), defaultContent),
        asString(payload.get("riskLevel")),
        zeroToNull(asLong(payload.get("triageRecordId"))),
        zeroToNull(asLong(payload.get("medicalRecordId")))
    );
  }

  private void auditNotificationProcessed(DomainEventEnvelope envelope, NotificationCreateRequest request) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("actorType", "SYSTEM");
    payload.put("actorId", 0L);
    payload.put("action", "NOTIFICATION_PROCESSED");
    payload.put("resourceType", "NOTIFICATION_EVENT");
    payload.put("resourceId", envelope.eventId());
    payload.put("outcome", "SUCCESS");
    payload.put("eventType", envelope.eventType());
    payload.put("doctorId", request.doctorId());
    payload.put("patientId", request.patientId());
    auditService.recordLocal(envelope.eventId() + ":notification", DomainEventNames.NOTIFICATION_CREATED,
        "notification-service", payload);
  }

  private Long asLong(Object value) {
    if (value == null || String.valueOf(value).isBlank()) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private Long zeroToNull(Long value) {
    return value == null || value == 0 ? null : value;
  }

  private String asString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
