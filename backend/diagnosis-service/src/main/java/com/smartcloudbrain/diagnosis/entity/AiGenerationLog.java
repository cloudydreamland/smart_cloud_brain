package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_generation_log")
public class AiGenerationLog extends BaseEntity {

  @Column(name = "task_type")
  private String taskType;
  @Column(name = "biz_id")
  private Long bizId;
  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "prompt_template_id")
  private Long promptTemplateId;
  @Column(name = "request_summary")
  private String requestSummary;
  @Column(name = "response_summary")
  private String responseSummary;
  @Column(name = "raw_result_json")
  private String rawResultJson;
  private String status;
  @Column(name = "error_message")
  private String errorMessage;
  @Column(name = "duration_ms")
  private Integer durationMs;
}
