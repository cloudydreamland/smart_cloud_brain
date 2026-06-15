package com.smartcloudbrain.prescription.entity;

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

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getMedicalRecordId() {
    return medicalRecordId;
  }

  public void setMedicalRecordId(Long medicalRecordId) {
    this.medicalRecordId = medicalRecordId;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}


