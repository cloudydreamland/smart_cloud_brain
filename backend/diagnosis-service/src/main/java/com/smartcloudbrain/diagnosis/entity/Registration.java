package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration")
public class Registration extends BaseEntity {

  @Column(name = "patient_id")
  private Long patientId;
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
}
