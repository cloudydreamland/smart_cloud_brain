package com.smartcloudbrain.auth.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.common.event.RabbitTopology;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);
  private static final String SOURCE_SERVICE = "auth-service";

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public DomainEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  public void publishAudit(String eventType, Map<String, Object> payload) {
    try {
      DomainEventEnvelope envelope = new DomainEventEnvelope(
          UUID.randomUUID().toString(), eventType, Instant.now(), SOURCE_SERVICE, "", payload);
      rabbitTemplate.convertAndSend(
          RabbitTopology.DOMAIN_EXCHANGE,
          RabbitTopology.ROUTING_AUDIT_LOG,
          objectMapper.writeValueAsString(envelope)
      );
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize domain event", ex);
    } catch (RuntimeException ex) {
      log.warn("Failed to publish audit event {}: {}", eventType, ex.getMessage());
    }
  }
}
