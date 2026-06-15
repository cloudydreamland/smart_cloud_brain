package com.smartcloudbrain.notification.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.common.event.RabbitTopology;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionCheckedConsumer {

  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  public PrescriptionCheckedConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
    this.objectMapper = objectMapper;
    this.notificationService = notificationService;
  }

  @RabbitListener(queues = RabbitTopology.NOTIFICATION_DISPATCH_QUEUE)
  public void onMessage(String message) throws Exception {
    Map<String, Object> envelope = objectMapper.readValue(message, new TypeReference<>() {
    });
    if (!DomainEventNames.PRESCRIPTION_CHECKED.equals(envelope.get("eventType"))) {
      return;
    }
    Map<String, Object> payload = castMap(envelope.get("payload"));
    String riskLevel = asString(payload.get("riskLevel"));
    if (!List.of("HIGH", "MEDIUM").contains(riskLevel)) {
      return;
    }
    notificationService.create(new NotificationCreateRequest(
        asLong(payload.get("doctorId")),
        asLong(payload.get("patientId")),
        zeroToNull(asLong(payload.get("prescriptionId"))),
        asString(payload.getOrDefault("type", "PRESCRIPTION_HIGH_RISK")),
        asString(payload.getOrDefault("title", "AI prescription risk alert")),
        asString(payload.get("suggestions")),
        riskLevel
    ));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }

  private Long asLong(Object value) {
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
}
