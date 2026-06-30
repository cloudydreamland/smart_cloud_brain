package com.smartcloudbrain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_email_verification_code")
public class PatientEmailVerificationCode extends BaseEntity {

  private String email;
  private String phone;
  private String purpose;
  @Column(name = "code_hash")
  private String codeHash;
  @Column(name = "expires_at")
  private LocalDateTime expiresAt;
  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;
  @Column(name = "failed_attempts")
  private Integer failedAttempts;
  @Column(name = "last_sent_ip")
  private String lastSentIp;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public void setCodeHash(String codeHash) {
    this.codeHash = codeHash;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public LocalDateTime getConsumedAt() {
    return consumedAt;
  }

  public void setConsumedAt(LocalDateTime consumedAt) {
    this.consumedAt = consumedAt;
  }

  public Integer getFailedAttempts() {
    return failedAttempts;
  }

  public void setFailedAttempts(Integer failedAttempts) {
    this.failedAttempts = failedAttempts;
  }

  public String getLastSentIp() {
    return lastSentIp;
  }

  public void setLastSentIp(String lastSentIp) {
    this.lastSentIp = lastSentIp;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
