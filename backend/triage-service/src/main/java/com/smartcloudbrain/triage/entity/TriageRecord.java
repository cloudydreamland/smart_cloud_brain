package com.smartcloudbrain.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "triage_record")
public class TriageRecord extends BaseEntity {

  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "chief_complaint")
  private String chiefComplaint;
  @Column(name = "recommended_department")
  private String recommendedDepartment;
  @Column(name = "recommended_doctor_ids")
  private String recommendedDoctorIds;
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


