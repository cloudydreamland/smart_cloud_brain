package com.smartcloudbrain.medicalrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration")
public class Registration extends BaseEntity {

  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "owner_patient_id")
  private Long ownerPatientId;
  @Column(name = "subject_type")
  private String subjectType;
  @Column(name = "subject_id")
  private Long subjectId;
  @Column(name = "subject_name")
  private String subjectName;
  @Column(name = "subject_relationship")
  private String subjectRelationship;
  @Column(name = "subject_gender")
  private String subjectGender;
  @Column(name = "subject_age")
  private Integer subjectAge;
  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "department_id")
  private Long departmentId;
  @Column(name = "triage_record_id")
  private Long triageRecordId;
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

  public Long getOwnerPatientId() {
    return ownerPatientId;
  }

  public void setOwnerPatientId(Long ownerPatientId) {
    this.ownerPatientId = ownerPatientId;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public void setSubjectType(String subjectType) {
    this.subjectType = subjectType;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public void setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public void setSubjectName(String subjectName) {
    this.subjectName = subjectName;
  }

  public String getSubjectRelationship() {
    return subjectRelationship;
  }

  public void setSubjectRelationship(String subjectRelationship) {
    this.subjectRelationship = subjectRelationship;
  }

  public String getSubjectGender() {
    return subjectGender;
  }

  public void setSubjectGender(String subjectGender) {
    this.subjectGender = subjectGender;
  }

  public Integer getSubjectAge() {
    return subjectAge;
  }

  public void setSubjectAge(Integer subjectAge) {
    this.subjectAge = subjectAge;
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


