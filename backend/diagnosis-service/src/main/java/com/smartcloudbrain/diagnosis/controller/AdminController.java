package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.diagnosis.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.KnowledgeEntrySaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.PromptTemplateSaveRequest;
import com.smartcloudbrain.diagnosis.service.AdminCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final AdminCatalogService adminCatalogService;

  public AdminController(AdminCatalogService adminCatalogService) {
    this.adminCatalogService = adminCatalogService;
  }

  @GetMapping("/department/list")
  public Result<?> departments() {
    return Result.success(adminCatalogService.departments());
  }

  @PostMapping("/department/save")
  public Result<?> saveDepartment(@Valid @RequestBody DepartmentSaveRequest request) {
    return Result.success(adminCatalogService.saveDepartment(request));
  }

  @PostMapping("/doctor/save")
  public Result<?> saveDoctor(@Valid @RequestBody DoctorSaveRequest request) {
    return Result.success(adminCatalogService.saveDoctor(request));
  }

  @GetMapping("/drug/list")
  public Result<?> drugs() {
    return Result.success(adminCatalogService.drugs());
  }

  @PostMapping("/drug/save")
  public Result<?> saveDrug(@Valid @RequestBody DrugSaveRequest request) {
    return Result.success(adminCatalogService.saveDrug(request));
  }

  @GetMapping("/prompt-template/list")
  public Result<?> prompts() {
    return Result.success(adminCatalogService.prompts());
  }

  @PostMapping("/prompt-template/save")
  public Result<?> savePrompt(@Valid @RequestBody PromptTemplateSaveRequest request) {
    return Result.success(adminCatalogService.savePrompt(request));
  }

  @GetMapping("/knowledge/list")
  public Result<?> knowledgeEntries() {
    return Result.success(adminCatalogService.knowledgeEntries());
  }

  @PostMapping("/knowledge/save")
  public Result<?> saveKnowledgeEntry(@Valid @RequestBody KnowledgeEntrySaveRequest request) {
    return Result.success(adminCatalogService.saveKnowledgeEntry(request));
  }
}
