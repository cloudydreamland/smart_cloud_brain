package com.smartcloudbrain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.notification.entity.AuditLog;
import com.smartcloudbrain.notification.repository.AuditLogRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  @InjectMocks private AuditService auditService;

  @Test
  void recordNewEventSavesAuditLog() {
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-1", "AUTH_LOGIN", Instant.now(), "auth-service", "trace-1",
        Map.of("action", "AUTH_LOGIN", "actorType", "USER", "actorId", 42L,
            "resourceType", "SESSION", "resourceId", "sess-1", "outcome", "SUCCESS")
    );
    when(auditLogRepository.findByEventId("evt-1")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
      AuditLog log = inv.getArgument(0);
      log.setId(1L);
      return log;
    });

    AuditLog result = auditService.record(envelope);

    assertNotNull(result);
    assertEquals("evt-1", result.getEventId());
    assertEquals("AUTH_LOGIN", result.getEventType());
    assertEquals("auth-service", result.getSourceService());
    assertEquals("trace-1", result.getTraceId());
    assertEquals("USER", result.getActorType());
    assertEquals(42L, result.getActorId());
    assertEquals("AUTH_LOGIN", result.getAction());
    assertEquals("SESSION", result.getResourceType());
    assertEquals("sess-1", result.getResourceId());
    assertEquals("SUCCESS", result.getOutcome());
    assertNotNull(result.getDetailJson());
    verify(auditLogRepository).save(any(AuditLog.class));
  }

  @Test
  void recordDuplicateEventReturnsExisting() {
    AuditLog existing = new AuditLog();
    existing.setId(99L);
    existing.setEventId("evt-dup");
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-dup", "AUTH_LOGIN", Instant.now(), "auth-service", "", Map.of()
    );
    when(auditLogRepository.findByEventId("evt-dup")).thenReturn(Optional.of(existing));

    AuditLog result = auditService.record(envelope);

    assertEquals(99L, result.getId());
    verify(auditLogRepository, never()).save(any());
  }

  @Test
  void recordLocalDelegatesToRecord() {
    when(auditLogRepository.findByEventId("local-1")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
      AuditLog log = inv.getArgument(0);
      log.setId(10L);
      return log;
    });

    AuditLog result = auditService.recordLocal("local-1", "SYSTEM_EVENT", "notification-service",
        Map.of("action", "test"));

    assertEquals("local-1", result.getEventId());
    assertEquals("SYSTEM_EVENT", result.getEventType());
    assertEquals("notification-service", result.getSourceService());
  }

  @Test
  void toAuditLogHandlesNonMapPayload() {
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-2", "PRESCRIPTION_RISK", Instant.now(), "prescription-service", "",
        "raw-string-payload"
    );
    when(auditLogRepository.findByEventId("evt-2")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

    AuditLog result = auditService.record(envelope);

    // Non-map payload: action defaults to eventType, outcome defaults to SUCCESS
    assertEquals("PRESCRIPTION_RISK", result.getAction());
    assertEquals("SUCCESS", result.getOutcome());
    assertNotNull(result.getDetailJson());
    // detailJson should wrap non-map as {"payload":"raw-string-payload"}
    assertEquals("{\"payload\":\"raw-string-payload\"}", result.getDetailJson());
  }

  @Test
  void toAuditLogHandlesNullOccurredAt() {
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-5", "EVENT", null, "svc", "", Map.of()
    );
    when(auditLogRepository.findByEventId("evt-5")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

    AuditLog result = auditService.record(envelope);

    assertNotNull(result.getOccurredAt());
  }

  @Test
  void toAuditLogHandlesNullPayloadFieldValues() {
    // Use HashMap because Map.of() does not allow null values
    Map<String, Object> payload = new HashMap<>();
    payload.put("actorType", null);
    payload.put("resourceType", null);
    payload.put("resourceId", null);
    payload.put("actorId", null);
    payload.put("outcome", null);
    payload.put("action", null);

    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-6", "EVENT", Instant.now(), "svc", "", payload
    );
    when(auditLogRepository.findByEventId("evt-6")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

    AuditLog result = auditService.record(envelope);

    assertEquals("", result.getActorType());
    assertEquals("", result.getResourceType());
    assertEquals("", result.getResourceId());
    assertEquals(null, result.getActorId());
    // action defaults to eventType when null
    assertEquals("EVENT", result.getAction());
    // outcome defaults to SUCCESS when null
    assertEquals("SUCCESS", result.getOutcome());
  }

  @Test
  void toAuditLogHandlesActorIdAsString() {
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-4", "EVENT", Instant.now(), "svc", "",
        Map.of("actorId", "789")
    );
    when(auditLogRepository.findByEventId("evt-4")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

    AuditLog result = auditService.record(envelope);

    assertEquals(789L, result.getActorId());
  }

  @Test
  void toAuditLogHandlesBlankSourceDefaultsToUnknown() {
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-7", "EVENT", Instant.now(), "", "", Map.of()
    );
    when(auditLogRepository.findByEventId("evt-7")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

    AuditLog result = auditService.record(envelope);

    assertEquals("unknown", result.getSourceService());
  }

  @Test
  void toAuditLogHandlesBlankActorIdReturnsNull() {
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "evt-8", "EVENT", Instant.now(), "svc", "",
        Map.of("actorId", "  ")
    );
    when(auditLogRepository.findByEventId("evt-8")).thenReturn(Optional.empty());
    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

    AuditLog result = auditService.record(envelope);

    assertEquals(null, result.getActorId());
  }
}
