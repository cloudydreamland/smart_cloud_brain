package com.smartcloudbrain.prescription.entity;

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
  private String diagnosis;
  @Column(name = "past_history")
  private String pastHistory;

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public Long getOwnerPatientId() {
    return ownerPatientId;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public String getSubjectRelationship() {
    return subjectRelationship;
  }

  public String getSubjectGender() {
    return subjectGender;
  }

  public Integer getSubjectAge() {
    return subjectAge;
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

  public String getDiagnosis() {
    return diagnosis;
  }

  public String getPastHistory() {
    return pastHistory;
  }
}
