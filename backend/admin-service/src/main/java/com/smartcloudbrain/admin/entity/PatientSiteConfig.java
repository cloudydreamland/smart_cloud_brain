package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_site_config")
public class PatientSiteConfig extends BaseEntity {

  @Column(name = "config_key")
  private String configKey;
  @Column(name = "config_json", columnDefinition = "TEXT")
  private String configJson;
  private String status;
  private Integer version;
  private String remark;
  @Column(name = "created_by")
  private Long createdBy;
  @Column(name = "updated_by")
  private Long updatedBy;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getConfigKey() { return configKey; }
  public void setConfigKey(String configKey) { this.configKey = configKey; }
  public String getConfigJson() { return configJson; }
  public void setConfigJson(String configJson) { this.configJson = configJson; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
