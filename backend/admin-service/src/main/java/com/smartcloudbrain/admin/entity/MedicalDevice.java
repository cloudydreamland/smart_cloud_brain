package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_device")
public class MedicalDevice extends BaseEntity {

  @Column(name = "device_code")
  private String deviceCode;
  private String name;
  private String category;
  @Column(name = "department_id")
  private Long departmentId;
  private String location;
  private String status;
  @Column(name = "purchase_date")
  private LocalDate purchaseDate;
  @Column(name = "last_maintenance_at")
  private LocalDateTime lastMaintenanceAt;
  private String remark;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getDeviceCode() { return deviceCode; }
  public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public Long getDepartmentId() { return departmentId; }
  public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDate getPurchaseDate() { return purchaseDate; }
  public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
  public LocalDateTime getLastMaintenanceAt() { return lastMaintenanceAt; }
  public void setLastMaintenanceAt(LocalDateTime lastMaintenanceAt) { this.lastMaintenanceAt = lastMaintenanceAt; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
