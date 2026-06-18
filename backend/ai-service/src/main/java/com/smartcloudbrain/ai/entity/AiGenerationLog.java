package com.smartcloudbrain.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_generation_log")
public class AiGenerationLog extends BaseEntity {

  @Column(name = "task_type")
  private String taskType;
  private String provider;
  private String model;
  @Column(name = "request_id")
  private String requestId;
  @Column(name = "input_summary")
  private String inputSummary;
  @Column(name = "output_summary")
  private String outputSummary;
  private Boolean success;
  @Column(name = "status", nullable = false)
  private String status;
  @Column(name = "error_message")
  private String errorMessage;
  @Column(name = "latency_ms")
  private Long latencyMs;
  @Column(name = "prompt_template_id")
  private Long promptTemplateId;
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public void setInputSummary(String inputSummary) {
    this.inputSummary = inputSummary;
  }

  public void setOutputSummary(String outputSummary) {
    this.outputSummary = outputSummary;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public void setLatencyMs(Long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public void setPromptTemplateId(Long promptTemplateId) {
    this.promptTemplateId = promptTemplateId;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
