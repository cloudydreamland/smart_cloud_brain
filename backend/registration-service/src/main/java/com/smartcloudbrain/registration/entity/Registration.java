package com.smartcloudbrain.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration")
public class Registration extends BaseEntity {

  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "visitor_id")
  private Long visitorId;
  @Column(name = "visitor_type")
  private String visitorType;
  @Column(name = "visitor_name")
  private String visitorName;
  @Column(name = "visitor_relationship")
  private String visitorRelationship;
  @Column(name = "visitor_gender")
  private String visitorGender;
  @Column(name = "visitor_age")
  private Integer visitorAge;
  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "department_id")
  private Long departmentId;
  @Column(name = "triage_record_id")
  private Long triageRecordId;
  @Column(name = "slot_id")
  private Long slotId;
  @Column(name = "appointment_time")
  private LocalDateTime appointmentTime;
  private String status;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getVisitorId() {
    return visitorId;
  }

  public void setVisitorId(Long visitorId) {
    this.visitorId = visitorId;
  }

  public String getVisitorType() {
    return visitorType;
  }

  public void setVisitorType(String visitorType) {
    this.visitorType = visitorType;
  }

  public String getVisitorName() {
    return visitorName;
  }

  public void setVisitorName(String visitorName) {
    this.visitorName = visitorName;
  }

  public String getVisitorRelationship() {
    return visitorRelationship;
  }

  public void setVisitorRelationship(String visitorRelationship) {
    this.visitorRelationship = visitorRelationship;
  }

  public String getVisitorGender() {
    return visitorGender;
  }

  public void setVisitorGender(String visitorGender) {
    this.visitorGender = visitorGender;
  }

  public Integer getVisitorAge() {
    return visitorAge;
  }

  public void setVisitorAge(Integer visitorAge) {
    this.visitorAge = visitorAge;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
  }

  public Long getTriageRecordId() {
    return triageRecordId;
  }

  public void setTriageRecordId(Long triageRecordId) {
    this.triageRecordId = triageRecordId;
  }

  public Long getSlotId() {
    return slotId;
  }

  public void setSlotId(Long slotId) {
    this.slotId = slotId;
  }

  public LocalDateTime getAppointmentTime() {
    return appointmentTime;
  }

  public void setAppointmentTime(LocalDateTime appointmentTime) {
    this.appointmentTime = appointmentTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}


