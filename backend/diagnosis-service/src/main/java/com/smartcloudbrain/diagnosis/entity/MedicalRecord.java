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
}
