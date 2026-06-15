package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_entry")
public class KnowledgeEntry extends BaseEntity {

  private String title;
  private String symptoms;
  @Column(name = "risk_signals")
  private String riskSignals;
  private String advice;
  @Column(name = "department_code")
  private String departmentCode;
  private String status;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSymptoms() {
    return symptoms;
  }

  public void setSymptoms(String symptoms) {
    this.symptoms = symptoms;
  }

  public String getRiskSignals() {
    return riskSignals;
  }

  public void setRiskSignals(String riskSignals) {
    this.riskSignals = riskSignals;
  }

  public String getAdvice() {
    return advice;
  }

  public void setAdvice(String advice) {
    this.advice = advice;
  }

  public String getDepartmentCode() {
    return departmentCode;
  }

  public void setDepartmentCode(String departmentCode) {
    this.departmentCode = departmentCode;
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


