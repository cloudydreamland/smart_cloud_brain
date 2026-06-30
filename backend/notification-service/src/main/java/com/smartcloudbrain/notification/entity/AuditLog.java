package com.smartcloudbrain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

  @Column(name = "event_id", nullable = false, unique = true)
  private String eventId;
  @Column(name = "event_type", nullable = false)
  private String eventType;
  @Column(name = "source_service", nullable = false)
  private String sourceService;
  @Column(name = "trace_id")
  private String traceId;
  @Column(name = "actor_type")
  private String actorType;
  @Column(name = "actor_id")
  private Long actorId;
  @Column(nullable = false)
  private String action;
  @Column(name = "resource_type")
  private String resourceType;
  @Column(name = "resource_id")
  private String resourceId;
  @Column(nullable = false)
  private String outcome;
  @Column(name = "detail_json")
  private String detailJson;
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

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

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getActorType() {
    return actorType;
  }

  public void setActorType(String actorType) {
    this.actorType = actorType;
  }

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long actorId) {
    this.actorId = actorId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getDetailJson() {
    return detailJson;
  }

  public void setDetailJson(String detailJson) {
    this.detailJson = detailJson;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
