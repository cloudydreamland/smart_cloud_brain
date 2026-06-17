package com.smartcloudbrain.prescription.entity;

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
  private Integer days;
  private String remark;

  public Long getPrescriptionId() {
    return prescriptionId;
  }

  public void setPrescriptionId(Long prescriptionId) {
    this.prescriptionId = prescriptionId;
  }

  public String getDrugName() {
    return drugName;
  }

  public void setDrugName(String drugName) {
    this.drugName = drugName;
  }

  public String getDosage() {
    return dosage;
  }

  public void setDosage(String dosage) {
    this.dosage = dosage;
  }

  public String getFrequency() {
    return frequency;
  }

  public void setFrequency(String frequency) {
    this.frequency = frequency;
  }

  public String getUsageMethod() {
    return usageMethod;
  }

  public void setUsageMethod(String usageMethod) {
    this.usageMethod = usageMethod;
  }

  public Integer getDays() {
    return days;
  }

  public void setDays(Integer days) {
    this.days = days;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}


