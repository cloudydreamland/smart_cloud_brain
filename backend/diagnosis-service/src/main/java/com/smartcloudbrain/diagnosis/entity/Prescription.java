package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription")
public class Prescription extends BaseEntity {

  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "medical_record_id")
  private Long medicalRecordId;
  @Column(name = "risk_level")
  private String riskLevel;
  private String status;
}
