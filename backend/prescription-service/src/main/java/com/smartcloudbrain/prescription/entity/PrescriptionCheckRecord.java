package com.smartcloudbrain.prescription.entity;

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

  public Long getPrescriptionId() {
    return prescriptionId;
  }

  public void setPrescriptionId(Long prescriptionId) {
    this.prescriptionId = prescriptionId;
  }

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

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }

  public String getSuggestions() {
    return suggestions;
  }

  public void setSuggestions(String suggestions) {
    this.suggestions = suggestions;
  }

  public String getInteractions() {
    return interactions;
  }

  public void setInteractions(String interactions) {
    this.interactions = interactions;
  }

  public String getAiResultJson() {
    return aiResultJson;
  }

  public void setAiResultJson(String aiResultJson) {
    this.aiResultJson = aiResultJson;
  }
}


