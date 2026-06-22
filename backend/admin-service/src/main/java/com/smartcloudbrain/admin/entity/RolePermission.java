package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_permission")
public class RolePermission extends BaseEntity {

  private String role;
  @Column(name = "permission_key")
  private String permissionKey;
  private Boolean enabled;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getPermissionKey() { return permissionKey; }
  public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
  public Boolean getEnabled() { return enabled; }
  public void setEnabled(Boolean enabled) { this.enabled = enabled; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
