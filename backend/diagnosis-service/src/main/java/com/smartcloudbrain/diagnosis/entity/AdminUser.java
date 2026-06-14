package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_user")
public class AdminUser extends BaseEntity {

  private String username;
  @Column(name = "password_hash")
  private String passwordHash;
  private String name;
  private String status;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
