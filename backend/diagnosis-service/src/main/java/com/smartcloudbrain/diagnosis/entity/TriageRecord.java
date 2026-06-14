package com.smartcloudbrain.diagnosis.entity;

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
}
