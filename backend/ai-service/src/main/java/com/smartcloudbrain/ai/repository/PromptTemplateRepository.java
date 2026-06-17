package com.smartcloudbrain.ai.repository;

import com.smartcloudbrain.ai.entity.PromptTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
  List<PromptTemplate> findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc(String taskType, String departmentCode, Boolean enabled);
}
