package com.smartcloudbrain.notification.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.notification.service.AuditService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogConsumerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  @Mock private AuditService auditService;

  @Test
  void parsesEnvelopeAndRecordsAuditLog() throws Exception {
    AuditLogConsumer consumer = new AuditLogConsumer(objectMapper, auditService);
    DomainEventEnvelope envelope = new DomainEventEnvelope(
        "audit-1",
        DomainEventNames.AUTH_LOGIN,
        Instant.now(),
        "auth-service",
        "",
        Map.of("action", "AUTH_LOGIN")
    );

    consumer.onMessage(objectMapper.writeValueAsString(envelope));

    ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
    verify(auditService).record(captor.capture());
    assertEquals("audit-1", captor.getValue().eventId());
    assertEquals(DomainEventNames.AUTH_LOGIN, captor.getValue().eventType());
  }
}
