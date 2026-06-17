package com.smartcloudbrain.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient")
public class Patient extends BaseEntity {

  private String gender;
  private Integer age;
  @Column(name = "allergy_history")
  private String allergyHistory;
  @Column(name = "past_history")
  private String pastHistory;

  public String getGender() {
    return gender;
  }

  public Integer getAge() {
    return age;
  }

  public String getAllergyHistory() {
    return allergyHistory;
  }

  public String getPastHistory() {
    return pastHistory;
  }
}
