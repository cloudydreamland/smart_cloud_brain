package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {

  private String name;
  private String phone;
  @Column(name = "password_hash")
  private String passwordHash;
  private String gender;
  private Integer age;
  @Column(name = "allergy_history")
  private String allergyHistory;
  @Column(name = "past_history")
  private String pastHistory;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
