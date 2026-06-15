package com.smartcloudbrain.prescription.entity;

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
}
