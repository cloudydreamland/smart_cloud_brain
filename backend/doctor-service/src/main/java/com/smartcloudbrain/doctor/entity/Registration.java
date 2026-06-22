package com.smartcloudbrain.doctor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration")
public class Registration {

  @Id
  private Long id;
  @Column(name = "slot_id")
  private Long slotId;
  private String status;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSlotId() { return slotId; }
  public void setSlotId(Long slotId) { this.slotId = slotId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
