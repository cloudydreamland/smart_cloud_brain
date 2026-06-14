package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription_check_record")
public class PrescriptionCheckRecord extends BaseEntity {

  @Column(name = "prescription_id")
  private Long prescriptionId;
  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "risk_level")
  private String riskLevel;
  private String suggestions;
  private String interactions;
  @Column(name = "ai_result_json")
  private String aiResultJson;
}
