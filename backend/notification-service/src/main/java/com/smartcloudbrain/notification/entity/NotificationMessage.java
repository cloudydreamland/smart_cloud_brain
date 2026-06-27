package com.smartcloudbrain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_message")
public class NotificationMessage extends BaseEntity {

  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "prescription_id")
  private Long prescriptionId;
  @Column(name = "triage_record_id")
  private Long triageRecordId;
  @Column(name = "medical_record_id")
  private Long medicalRecordId;
  private String type;
  private String title;
  private String content;
  @Column(name = "risk_level")
  private String riskLevel;
  @Column(name = "read_status")
  private String readStatus;
  @Column(name = "handle_status")
  private String handleStatus;
  @Column(name = "handled_at")
  private LocalDateTime handledAt;

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

  public Long getTriageRecordId() {
    return triageRecordId;
  }

  public void setTriageRecordId(Long triageRecordId) {
    this.triageRecordId = triageRecordId;
  }

  public Long getMedicalRecordId() {
    return medicalRecordId;
  }

  public void setMedicalRecordId(Long medicalRecordId) {
    this.medicalRecordId = medicalRecordId;
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

  public String getHandleStatus() {
    return handleStatus;
  }

  public void setHandleStatus(String handleStatus) {
    this.handleStatus = handleStatus;
  }

  public LocalDateTime getHandledAt() {
    return handledAt;
  }

  public void setHandledAt(LocalDateTime handledAt) {
    this.handledAt = handledAt;
  }
}

