package com.smartcloudbrain.notification.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.notification.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogConsumerAdditionalTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  @Mock private AuditService auditService;

  @Test
  void onMessage_invalidJson_throwsException() {
    AuditLogConsumer consumer = new AuditLogConsumer(objectMapper, auditService);
    assertThrows(Exception.class, () -> consumer.onMessage("not valid json{{{"));
    verify(auditService, never()).record(null);
  }

  @Test
  void onMessage_emptyJson_doesNotThrow() throws Exception {
    AuditLogConsumer consumer = new AuditLogConsumer(objectMapper, auditService);
    // Empty JSON {} deserializes to an empty map — no exception expected
    consumer.onMessage("{}");
  }
}
