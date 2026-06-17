package com.smartcloudbrain.ai.service;

import com.smartcloudbrain.ai.entity.PromptTemplate;
import com.smartcloudbrain.ai.repository.PromptTemplateRepository;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptTemplateService {

  private final PromptTemplateRepository promptTemplateRepository;

  public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
    this.promptTemplateRepository = promptTemplateRepository;
  }

  public PromptResolveResponse resolve(String taskType, String departmentCode) {
    String normalizedDepartment = StringUtils.hasText(departmentCode) ? departmentCode : "GENERAL";
    PromptTemplate template = firstEnabled(taskType, normalizedDepartment);
    if (template == null && !"GENERAL".equalsIgnoreCase(normalizedDepartment)) {
      template = firstEnabled(taskType, "GENERAL");
    }
    if (template == null) {
      throw new BusinessException(600, "No enabled prompt template for taskType=" + taskType + ", departmentCode=" + normalizedDepartment);
    }
    return new PromptResolveResponse(
        template.getId(),
        template.getTaskType(),
        template.getDepartmentCode(),
        template.getTemplateName(),
        template.getTemplateContent(),
        template.getOutputSchema(),
        template.getVersion()
    );
  }

  private PromptTemplate firstEnabled(String taskType, String departmentCode) {
    List<PromptTemplate> templates = promptTemplateRepository
        .findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc(taskType, departmentCode, true);
    return templates.isEmpty() ? null : templates.get(0);
  }
}
