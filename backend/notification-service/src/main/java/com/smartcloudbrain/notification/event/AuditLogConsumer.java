package com.smartcloudbrain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.common.event.RabbitTopology;
import com.smartcloudbrain.notification.service.AuditService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLogConsumer {

  private final ObjectMapper objectMapper;
  private final AuditService auditService;

  public AuditLogConsumer(ObjectMapper objectMapper, AuditService auditService) {
    this.objectMapper = objectMapper;
    this.auditService = auditService;
  }

  @RabbitListener(queues = RabbitTopology.AUDIT_LOG_QUEUE)
  public void onMessage(String message) throws Exception {
    auditService.record(objectMapper.readValue(message, DomainEventEnvelope.class));
  }
}
