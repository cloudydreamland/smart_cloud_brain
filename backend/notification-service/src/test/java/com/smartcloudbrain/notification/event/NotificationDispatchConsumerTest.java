package com.smartcloudbrain.notification.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.service.AuditService;
import com.smartcloudbrain.notification.service.NotificationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchConsumerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  @Mock private NotificationService notificationService;
  @Mock private AuditService auditService;

  @Test
  void createsNotificationFromRegistrationEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);
    payload.put("registrationId", 9L);
    payload.put("type", "REGISTRATION_CREATED");
    payload.put("title", "挂号成功提醒");
    payload.put("content", "患者已完成挂号");

    consumer.onMessage(message(DomainEventNames.REGISTRATION_CREATED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals(2L, captor.getValue().doctorId());
    assertEquals(1L, captor.getValue().patientId());
    assertEquals("REGISTRATION_CREATED", captor.getValue().type());
    verify(auditService).recordLocal(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.eq(DomainEventNames.NOTIFICATION_CREATED),
        org.mockito.ArgumentMatchers.eq("notification-service"),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  void skipsLowRiskPrescriptionEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = Map.of("doctorId", 2L, "patientId", 1L, "riskLevel", "LOW");

    consumer.onMessage(message(DomainEventNames.PRESCRIPTION_CHECKED, payload));

    verify(notificationService, never()).create(org.mockito.ArgumentMatchers.any());
    verify(auditService, never()).recordLocal(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any());
  }

  private String message(String eventType, Map<String, Object> payload) throws Exception {
    return objectMapper.writeValueAsString(new DomainEventEnvelope(
        "event-1", eventType, Instant.now(), "test-service", "", payload));
  }
}
