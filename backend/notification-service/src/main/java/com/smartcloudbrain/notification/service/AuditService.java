package com.smartcloudbrain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.notification.entity.AuditLog;
import com.smartcloudbrain.notification.repository.AuditLogRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public AuditLog record(DomainEventEnvelope envelope) {
    return auditLogRepository.findByEventId(envelope.eventId())
        .orElseGet(() -> auditLogRepository.save(toAuditLog(envelope)));
  }

  @Transactional
  public AuditLog recordLocal(String eventId, String eventType, String source, Map<String, Object> payload) {
    return record(new DomainEventEnvelope(eventId, eventType, Instant.now(), source, "", payload));
  }

  @SuppressWarnings("unchecked")
  private AuditLog toAuditLog(DomainEventEnvelope envelope) {
    Map<String, Object> payload = envelope.payload() instanceof Map<?, ?> map
        ? (Map<String, Object>) map
        : Map.of("payload", envelope.payload());
    AuditLog log = new AuditLog();
    log.setEventId(envelope.eventId());
    log.setEventType(envelope.eventType());
    log.setSourceService(defaultText(envelope.source(), "unknown"));
    log.setTraceId(envelope.traceId());
    log.setActorType(asString(payload.get("actorType")));
    log.setActorId(asLong(payload.get("actorId")));
    log.setAction(defaultText(asString(payload.get("action")), envelope.eventType()));
    log.setResourceType(asString(payload.get("resourceType")));
    log.setResourceId(asString(payload.get("resourceId")));
    log.setOutcome(defaultText(asString(payload.get("outcome")), "SUCCESS"));
    log.setDetailJson(toJson(payload));
    log.setOccurredAt(envelope.occurredAt() == null ? Instant.now() : envelope.occurredAt());
    return log;
  }

  private String toJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return "{}";
    }
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

  private String asString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
