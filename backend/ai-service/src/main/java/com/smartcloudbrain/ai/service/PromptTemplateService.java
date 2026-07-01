package com.smartcloudbrain.ai.service;

import com.smartcloudbrain.ai.entity.Department;
import com.smartcloudbrain.ai.entity.PromptTemplate;
import com.smartcloudbrain.ai.repository.DepartmentRepository;
import com.smartcloudbrain.ai.repository.PromptTemplateRepository;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptTemplateService {

  private final PromptTemplateRepository promptTemplateRepository;
  private final DepartmentRepository departmentRepository;

  public PromptTemplateService(
      PromptTemplateRepository promptTemplateRepository,
      DepartmentRepository departmentRepository
  ) {
    this.promptTemplateRepository = promptTemplateRepository;
    this.departmentRepository = departmentRepository;
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
    String content = template.getTemplateContent();
    if (content != null && content.contains("{{departmentList}}")) {
      content = content.replace("{{departmentList}}", resolveDepartmentList());
    }
    return new PromptResolveResponse(
        template.getId(),
        template.getTaskType(),
        template.getDepartmentCode(),
        template.getTemplateName(),
        content,
        template.getOutputSchema(),
        template.getVersion()
    );
  }

  private String resolveDepartmentList() {
    List<Department> departments = departmentRepository.findAllByOrderByIdAsc();
    return departments.stream()
        .map(d -> d.getName() + "(" + d.getCode() + ")")
        .collect(Collectors.joining("、"));
  }

  private PromptTemplate firstEnabled(String taskType, String departmentCode) {
    List<PromptTemplate> templates = promptTemplateRepository
        .findByTaskTypeAndDepartmentCodeAndEnabledOrderByIdDesc(taskType, departmentCode, true);
    return templates.isEmpty() ? null : templates.get(0);
  }
}
