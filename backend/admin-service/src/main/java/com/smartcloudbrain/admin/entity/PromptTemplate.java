package com.smartcloudbrain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_template")
public class PromptTemplate extends BaseEntity {

  @Column(name = "task_type")
  private String taskType;
  @Column(name = "department_code")
  private String departmentCode;
  @Column(name = "template_name")
  private String templateName;
  @Column(name = "template_content")
  private String templateContent;
  @Column(name = "output_schema")
  private String outputSchema;
  private String version;
  private Boolean enabled;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public String getDepartmentCode() {
    return departmentCode;
  }

  public void setDepartmentCode(String departmentCode) {
    this.departmentCode = departmentCode;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public String getTemplateContent() {
    return templateContent;
  }

  public void setTemplateContent(String templateContent) {
    this.templateContent = templateContent;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}


