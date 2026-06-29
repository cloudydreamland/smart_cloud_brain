package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_recommendation")
public class PatientRecommendation extends BaseEntity {

  @Column(name = "recommend_type")
  private String recommendType;
  @Column(name = "target_id")
  private Long targetId;
  private String title;
  private String description;
  @Column(name = "image_url")
  private String imageUrl;
  @Column(name = "image_object_key")
  private String imageObjectKey;
  private Integer sort;
  private String status;
  private Boolean deleted;
  @Column(name = "created_by")
  private Long createdBy;
  @Column(name = "updated_by")
  private Long updatedBy;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getRecommendType() {
    return recommendType;
  }

  public void setRecommendType(String recommendType) {
    this.recommendType = recommendType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getImageObjectKey() {
    return imageObjectKey;
  }

  public void setImageObjectKey(String imageObjectKey) {
    this.imageObjectKey = imageObjectKey;
  }

  public Integer getSort() {
    return sort;
  }

  public void setSort(Integer sort) {
    this.sort = sort;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
