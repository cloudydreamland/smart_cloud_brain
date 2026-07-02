package com.smartcloudbrain.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "triage_record")
public class TriageRecord extends BaseEntity {

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
  @Column(name = "chief_complaint")
  private String chiefComplaint;
  @Column(name = "recommended_department")
  private String recommendedDepartment;
  @Column(name = "recommended_doctor_ids")
  private String recommendedDoctorIds;
  @Column(name = "assigned_doctor_id")
  private Long assignedDoctorId;
  private String reason;
  @Column(name = "ai_result_json")
  private String aiResultJson;
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

  public String getChiefComplaint() {
    return chiefComplaint;
  }

  public void setChiefComplaint(String chiefComplaint) {
    this.chiefComplaint = chiefComplaint;
  }

  public String getRecommendedDepartment() {
    return recommendedDepartment;
  }

  public void setRecommendedDepartment(String recommendedDepartment) {
    this.recommendedDepartment = recommendedDepartment;
  }

  public String getRecommendedDoctorIds() {
    return recommendedDoctorIds;
  }

  public void setRecommendedDoctorIds(String recommendedDoctorIds) {
    this.recommendedDoctorIds = recommendedDoctorIds;
  }

  public Long getAssignedDoctorId() {
    return assignedDoctorId;
  }

  public void setAssignedDoctorId(Long assignedDoctorId) {
    this.assignedDoctorId = assignedDoctorId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getAiResultJson() {
    return aiResultJson;
  }

  public void setAiResultJson(String aiResultJson) {
    this.aiResultJson = aiResultJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}


