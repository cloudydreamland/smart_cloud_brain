package com.smartcloudbrain.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient_visitor")
public class PatientVisitor extends BaseEntity {

  @Column(name = "owner_patient_id")
  private Long ownerPatientId;
  private String name;
  private String relationship;
  private String gender;
  private Integer age;
  @Column(name = "allergy_history")
  private String allergyHistory;
  @Column(name = "past_history")
  private String pastHistory;

  public Long getOwnerPatientId() {
    return ownerPatientId;
  }

  public void setOwnerPatientId(Long ownerPatientId) {
    this.ownerPatientId = ownerPatientId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRelationship() {
    return relationship;
  }

  public void setRelationship(String relationship) {
    this.relationship = relationship;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getAllergyHistory() {
    return allergyHistory;
  }

  public void setAllergyHistory(String allergyHistory) {
    this.allergyHistory = allergyHistory;
  }

  public String getPastHistory() {
    return pastHistory;
  }

  public void setPastHistory(String pastHistory) {
    this.pastHistory = pastHistory;
  }
}
