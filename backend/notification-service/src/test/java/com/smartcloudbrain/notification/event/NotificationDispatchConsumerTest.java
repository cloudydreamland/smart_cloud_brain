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

  @Test
  void createsNotificationFromHighRiskPrescriptionEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);
    payload.put("riskLevel", "HIGH");
    payload.put("suggestions", "请复查用药剂量");

    consumer.onMessage(message(DomainEventNames.PRESCRIPTION_CHECKED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals(2L, captor.getValue().doctorId());
    assertEquals("PRESCRIPTION_HIGH_RISK", captor.getValue().type());
    verify(auditService).recordLocal(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.eq(DomainEventNames.NOTIFICATION_CREATED),
        org.mockito.ArgumentMatchers.eq("notification-service"),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createsNotificationFromMediumRiskPrescriptionEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);
    payload.put("riskLevel", "MEDIUM");
    payload.put("suggestions", "建议调整剂量");

    consumer.onMessage(message(DomainEventNames.PRESCRIPTION_CHECKED, payload));

    verify(notificationService).create(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createsNotificationFromTriageAssignedEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);
    payload.put("triageRecordId", 5L);

    consumer.onMessage(message(DomainEventNames.TRIAGE_ASSIGNED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals("TRIAGE_ASSIGN", captor.getValue().type());
    assertEquals(5L, captor.getValue().triageRecordId());
    assertEquals("分诊分配提醒", captor.getValue().title());
  }

  @Test
  void createsNotificationFromRegistrationCancelledEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);

    consumer.onMessage(message(DomainEventNames.REGISTRATION_CANCELLED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals("REGISTRATION_CANCELLED", captor.getValue().type());
    assertEquals("挂号取消提醒", captor.getValue().title());
  }

  @Test
  void createsNotificationFromSchedulePublishedEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 0L);
    payload.put("scheduleId", 10L);

    consumer.onMessage(message(DomainEventNames.SCHEDULE_PUBLISHED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals("SCHEDULE_PUBLISHED", captor.getValue().type());
    assertEquals("排班发布提醒", captor.getValue().title());
  }

  @Test
  void createsNotificationFromScheduleCancelledEvent() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 0L);
    payload.put("scheduleId", 10L);

    consumer.onMessage(message(DomainEventNames.SCHEDULE_CANCELLED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals("SCHEDULE_CANCELLED", captor.getValue().type());
    assertEquals("排班取消提醒", captor.getValue().title());
  }

  @Test
  void skipsMessageWhenDoctorIdIsNull() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("patientId", 1L);

    consumer.onMessage(message(DomainEventNames.REGISTRATION_CREATED, payload));

    verify(notificationService, never()).create(org.mockito.ArgumentMatchers.any());
    verify(auditService, never()).recordLocal(org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any());
  }

  @Test
  void skipsUnknownEventType() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);

    consumer.onMessage(message("unknown.event.type", payload));

    verify(notificationService, never()).create(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void usesDefaultContentWhenPayloadFieldsAreBlank() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);
    payload.put("type", "");
    payload.put("title", "");
    payload.put("content", "");

    consumer.onMessage(message(DomainEventNames.REGISTRATION_CREATED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals("REGISTRATION_CREATED", captor.getValue().type());
    assertEquals("挂号成功提醒", captor.getValue().title());
    assertEquals("患者已完成挂号，请关注候诊队列。", captor.getValue().content());
  }

  @Test
  void convertsZeroIdsToNull() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", 2L);
    payload.put("patientId", 1L);
    payload.put("prescriptionId", 0);
    payload.put("triageRecordId", 0);
    payload.put("medicalRecordId", 0);

    consumer.onMessage(message(DomainEventNames.REGISTRATION_CREATED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals(null, captor.getValue().prescriptionId());
    assertEquals(null, captor.getValue().triageRecordId());
    assertEquals(null, captor.getValue().medicalRecordId());
  }

  @Test
  void handlesStringTypedNumericValues() throws Exception {
    NotificationDispatchConsumer consumer = new NotificationDispatchConsumer(objectMapper, notificationService, auditService);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", "5");
    payload.put("patientId", "3");
    payload.put("prescriptionId", "10");
    payload.put("riskLevel", "HIGH");
    payload.put("suggestions", "建议");

    consumer.onMessage(message(DomainEventNames.PRESCRIPTION_CHECKED, payload));

    ArgumentCaptor<NotificationCreateRequest> captor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
    verify(notificationService).create(captor.capture());
    assertEquals(5L, captor.getValue().doctorId());
    assertEquals(3L, captor.getValue().patientId());
    assertEquals(10L, captor.getValue().prescriptionId());
  }

  private String message(String eventType, Map<String, Object> payload) throws Exception {
    return objectMapper.writeValueAsString(new DomainEventEnvelope(
        "event-1", eventType, Instant.now(), "test-service", "", payload));
  }
}
