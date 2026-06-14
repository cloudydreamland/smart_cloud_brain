package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor")
public class Doctor extends BaseEntity {

  private String name;
  private String phone;
  @Column(name = "password_hash")
  private String passwordHash;
  @Column(name = "department_id")
  private Long departmentId;
  private String title;
  private String specialty;
  private String status;
}
