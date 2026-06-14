package com.smartcloudbrain.diagnosis.entity;

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
}
