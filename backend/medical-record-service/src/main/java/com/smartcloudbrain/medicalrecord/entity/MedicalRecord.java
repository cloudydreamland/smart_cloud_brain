package com.smartcloudbrain.medicalrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_record")
public class MedicalRecord extends BaseEntity {

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
  @Column(name = "registration_id")
  private Long registrationId;
  @Column(name = "chief_complaint")
  private String chiefComplaint;
  @Column(name = "present_illness")
  private String presentIllness;
  @Column(name = "past_history")
  private String pastHistory;
  @Column(name = "physical_exam")
  private String physicalExam;
  private String diagnosis;
  @Column(name = "treatment_advice")
  private String treatmentAdvice;
  @Column(name = "ai_generated")
  private Boolean aiGenerated;

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

  public Long getRegistrationId() {
    return registrationId;
  }

  public void setRegistrationId(Long registrationId) {
    this.registrationId = registrationId;
  }

  public String getChiefComplaint() {
    return chiefComplaint;
  }

  public void setChiefComplaint(String chiefComplaint) {
    this.chiefComplaint = chiefComplaint;
  }

  public String getPresentIllness() {
    return presentIllness;
  }

  public void setPresentIllness(String presentIllness) {
    this.presentIllness = presentIllness;
  }

  public String getPastHistory() {
    return pastHistory;
  }

  public void setPastHistory(String pastHistory) {
    this.pastHistory = pastHistory;
  }

  public String getPhysicalExam() {
    return physicalExam;
  }

  public void setPhysicalExam(String physicalExam) {
    this.physicalExam = physicalExam;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public void setDiagnosis(String diagnosis) {
    this.diagnosis = diagnosis;
  }

  public String getTreatmentAdvice() {
    return treatmentAdvice;
  }

  public void setTreatmentAdvice(String treatmentAdvice) {
    this.treatmentAdvice = treatmentAdvice;
  }

  public Boolean getAiGenerated() {
    return aiGenerated;
  }

  public void setAiGenerated(Boolean aiGenerated) {
    this.aiGenerated = aiGenerated;
  }
}


