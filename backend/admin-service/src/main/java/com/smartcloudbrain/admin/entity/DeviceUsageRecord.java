package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_usage_record")
public class DeviceUsageRecord extends BaseEntity {

  @Column(name = "device_id")
  private Long deviceId;
  @Column(name = "usage_type")
  private String usageType;
  @Column(name = "used_by")
  private String usedBy;
  @Column(name = "patient_id")
  private Long patientId;
  @Column(name = "started_at")
  private LocalDateTime startedAt;
  @Column(name = "ended_at")
  private LocalDateTime endedAt;
  @Column(name = "result_status")
  private String resultStatus;
  private String remark;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public Long getDeviceId() { return deviceId; }
  public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
  public String getUsageType() { return usageType; }
  public void setUsageType(String usageType) { this.usageType = usageType; }
  public String getUsedBy() { return usedBy; }
  public void setUsedBy(String usedBy) { this.usedBy = usedBy; }
  public Long getPatientId() { return patientId; }
  public void setPatientId(Long patientId) { this.patientId = patientId; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getEndedAt() { return endedAt; }
  public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
  public String getResultStatus() { return resultStatus; }
  public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
