package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "medical_record")
public class MedicalRecord extends BaseEntity {

  @Column(name = "patient_id")
  private Long patientId;
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
