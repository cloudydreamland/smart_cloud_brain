package com.smartcloudbrain.diagnosis.entity;

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
}
