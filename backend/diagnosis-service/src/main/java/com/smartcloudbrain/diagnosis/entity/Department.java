package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "department")
public class Department extends BaseEntity {

  private String code;
  private String name;
  private String description;
}
