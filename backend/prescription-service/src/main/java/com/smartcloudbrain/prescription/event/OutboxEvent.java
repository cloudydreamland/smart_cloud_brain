package com.smartcloudbrain.prescription.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "event_id", nullable = false)
  private String eventId;
  @Column(name = "event_type", nullable = false)
  private String eventType;
  @Column(name = "source_service", nullable = false)
  private String sourceService;
  @Column(name = "routing_key", nullable = false)
  private String routingKey;
  @Column(name = "payload_json", nullable = false)
  private String payloadJson;
  @Column(nullable = false)
  private String status;
  @Column(nullable = false)
  private Integer attempts;
  @Column(name = "next_retry_at", nullable = false)
  private LocalDateTime nextRetryAt;
  @Column(name = "last_error")
  private String lastError;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  public Long getId() {
    return id;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getSourceService() {
    return sourceService;
  }

  public void setSourceService(String sourceService) {
    this.sourceService = sourceService;
  }

  public String getRoutingKey() {
    return routingKey;
  }

  public void setRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer attempts) {
    this.attempts = attempts;
  }

  public LocalDateTime getNextRetryAt() {
    return nextRetryAt;
  }

  public void setNextRetryAt(LocalDateTime nextRetryAt) {
    this.nextRetryAt = nextRetryAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
  }
}
