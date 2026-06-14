package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_message")
public class NotificationMessage extends BaseEntity {

  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "prescription_id")
  private Long prescriptionId;
  private String type;
  private String title;
  private String content;
  @Column(name = "risk_level")
  private String riskLevel;
  @Column(name = "read_status")
  private String readStatus;
}
