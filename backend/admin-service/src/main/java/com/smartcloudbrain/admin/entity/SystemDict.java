package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_dict")
public class SystemDict extends BaseEntity {

  @Column(name = "dict_type")
  private String dictType;
  @Column(name = "dict_key")
  private String dictKey;
  @Column(name = "dict_value")
  private String dictValue;
  private Integer sort;
  private String status;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getDictType() {
    return dictType;
  }

  public void setDictType(String dictType) {
    this.dictType = dictType;
  }

  public String getDictKey() {
    return dictKey;
  }

  public void setDictKey(String dictKey) {
    this.dictKey = dictKey;
  }

  public String getDictValue() {
    return dictValue;
  }

  public void setDictValue(String dictValue) {
    this.dictValue = dictValue;
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

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
