package com.smartcloudbrain.prescription.entity;

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

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getPrescriptionId() {
    return prescriptionId;
  }

  public void setPrescriptionId(Long prescriptionId) {
    this.prescriptionId = prescriptionId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public void setRiskLevel(String riskLevel) {
    this.riskLevel = riskLevel;
  }

  public String getReadStatus() {
    return readStatus;
  }

  public void setReadStatus(String readStatus) {
    this.readStatus = readStatus;
  }
}


