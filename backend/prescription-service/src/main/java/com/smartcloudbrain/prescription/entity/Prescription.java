package com.smartcloudbrain.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription")
public class Prescription extends BaseEntity {

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
  @Column(name = "medical_record_id")
  private Long medicalRecordId;
  @Column(name = "registration_id")
  private Long registrationId;
  @Column(name = "risk_level")
  private String riskLevel;
  private String status;

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

  public Long getMedicalRecordId() {
    return medicalRecordId;
  }

  public void setMedicalRecordId(Long medicalRecordId) {
    this.medicalRecordId = medicalRecordId;
  }

  public Long getRegistrationId() {
    return registrationId;
  }

  public void setRegistrationId(Long registrationId) {
    this.registrationId = registrationId;
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


