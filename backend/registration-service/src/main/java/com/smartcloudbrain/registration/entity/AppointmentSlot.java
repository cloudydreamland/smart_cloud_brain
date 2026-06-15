package com.smartcloudbrain.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_slot")
public class AppointmentSlot extends BaseEntity {

  @Column(name = "doctor_id")
  private Long doctorId;
  @Column(name = "department_id")
  private Long departmentId;
  @Column(name = "start_time")
  private LocalDateTime startTime;
  @Column(name = "end_time")
  private LocalDateTime endTime;
  private Integer capacity;
  @Column(name = "remaining_capacity")
  private Integer remainingCapacity;
  private String status;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public Long getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public Integer getRemainingCapacity() {
    return remainingCapacity;
  }

  public void setRemainingCapacity(Integer remainingCapacity) {
    this.remainingCapacity = remainingCapacity;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
