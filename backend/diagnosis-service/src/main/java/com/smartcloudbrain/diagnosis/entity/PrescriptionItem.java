package com.smartcloudbrain.diagnosis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription_item")
public class PrescriptionItem extends BaseEntity {

  @Column(name = "prescription_id")
  private Long prescriptionId;
  @Column(name = "drug_name")
  private String drugName;
  private String dosage;
  private String frequency;
  @Column(name = "usage_method")
  private String usageMethod;
}
