package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.PromptTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
  List<PromptTemplate> findByTaskTypeAndDepartmentCodeAndEnabled(String taskType, String departmentCode, Boolean enabled);
}
