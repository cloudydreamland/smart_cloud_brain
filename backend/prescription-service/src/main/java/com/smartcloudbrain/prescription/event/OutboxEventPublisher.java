package com.smartcloudbrain.prescription.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.event.DomainEventEnvelope;
import com.smartcloudbrain.common.event.RabbitTopology;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPublisher {

  private static final String SOURCE_SERVICE = "prescription-service";
  private final OutboxEventRepository outboxEventRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public OutboxEventPublisher(
      OutboxEventRepository outboxEventRepository,
      RabbitTemplate rabbitTemplate,
      ObjectMapper objectMapper
  ) {
    this.outboxEventRepository = outboxEventRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  public void enqueue(String eventType, String routingKey, Object payload) {
    String eventId = UUID.randomUUID().toString();
    DomainEventEnvelope envelope = new DomainEventEnvelope(eventId, eventType, Instant.now(), SOURCE_SERVICE, "", payload);
    OutboxEvent event = new OutboxEvent();
    event.setEventId(eventId);
    event.setEventType(eventType);
    event.setSourceService(SOURCE_SERVICE);
    event.setRoutingKey(routingKey);
    event.setPayloadJson(toJson(envelope));
    event.setStatus("PENDING");
    event.setAttempts(0);
    event.setNextRetryAt(LocalDateTime.now());
    event.setCreatedAt(LocalDateTime.now());
    outboxEventRepository.save(event);
  }

  @Scheduled(fixedDelayString = "${events.outbox.fixed-delay-ms:5000}")
  @Transactional
  public void publishPending() {
    List<OutboxEvent> events = outboxEventRepository.findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByIdAsc(
        List.of("PENDING", "FAILED"),
        LocalDateTime.now()
    );
    for (OutboxEvent event : events) {
      try {
        rabbitTemplate.convertAndSend(RabbitTopology.DOMAIN_EXCHANGE, event.getRoutingKey(), event.getPayloadJson());
        event.setStatus("SENT");
        event.setSentAt(LocalDateTime.now());
        event.setLastError(null);
      } catch (RuntimeException ex) {
        event.setStatus("FAILED");
        event.setAttempts(event.getAttempts() + 1);
        event.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(300, 5L * event.getAttempts())));
        event.setLastError(ex.getMessage());
      }
    }
  }

  private String toJson(DomainEventEnvelope envelope) {
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize domain event", ex);
    }
  }
}
