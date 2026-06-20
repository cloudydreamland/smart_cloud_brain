package com.smartcloudbrain.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "drug")
public class Drug extends BaseEntity {

  private String name;
  private String specification;
  private String contraindication;
  @Column(name = "interaction_rule")
  private String interactionRule;
  private String status;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSpecification() {
    return specification;
  }

  public void setSpecification(String specification) {
    this.specification = specification;
  }

  public String getContraindication() {
    return contraindication;
  }

  public void setContraindication(String contraindication) {
    this.contraindication = contraindication;
  }

  public String getInteractionRule() {
    return interactionRule;
  }

  public void setInteractionRule(String interactionRule) {
    this.interactionRule = interactionRule;
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
