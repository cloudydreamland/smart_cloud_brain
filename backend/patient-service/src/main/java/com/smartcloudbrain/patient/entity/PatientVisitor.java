package com.smartcloudbrain.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_visitor")
public class PatientVisitor extends BaseEntity {

  @Column(name = "owner_patient_id")
  private Long ownerPatientId;
  private String name;
  private String relationship;
  private String phone;
  private String gender;
  private Integer age;
  private String address;
  @Column(name = "emergency_contact")
  private String emergencyContact;
  @Column(name = "emergency_phone")
  private String emergencyPhone;
  @Column(name = "blood_type")
  private String bloodType;
  @Column(name = "height_cm")
  private Integer heightCm;
  @Column(name = "weight_kg")
  private BigDecimal weightKg;
  @Column(name = "allergy_history")
  private String allergyHistory;
  @Column(name = "past_history")
  private String pastHistory;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

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

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
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

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getEmergencyContact() {
    return emergencyContact;
  }

  public void setEmergencyContact(String emergencyContact) {
    this.emergencyContact = emergencyContact;
  }

  public String getEmergencyPhone() {
    return emergencyPhone;
  }

  public void setEmergencyPhone(String emergencyPhone) {
    this.emergencyPhone = emergencyPhone;
  }

  public String getBloodType() {
    return bloodType;
  }

  public void setBloodType(String bloodType) {
    this.bloodType = bloodType;
  }

  public Integer getHeightCm() {
    return heightCm;
  }

  public void setHeightCm(Integer heightCm) {
    this.heightCm = heightCm;
  }

  public BigDecimal getWeightKg() {
    return weightKg;
  }

  public void setWeightKg(BigDecimal weightKg) {
    this.weightKg = weightKg;
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

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
