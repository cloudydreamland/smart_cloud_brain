package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.KnowledgeEntrySaveRequest;
import com.smartcloudbrain.admin.dto.admin.PromptTemplateSaveRequest;
import com.smartcloudbrain.admin.service.AdminCatalogService;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
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
  private final CurrentUserService currentUserService;

  public AdminController(AdminCatalogService adminCatalogService, CurrentUserService currentUserService) {
    this.adminCatalogService = adminCatalogService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/department/list")
  public Result<?> departments() {
    requireAdmin();
    return Result.success(adminCatalogService.departments());
  }

  @PostMapping("/department/save")
  public Result<?> saveDepartment(@Valid @RequestBody DepartmentSaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.saveDepartment(request));
  }

  @PostMapping("/doctor/save")
  public Result<?> saveDoctor(@Valid @RequestBody DoctorSaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.saveDoctor(request));
  }

  @GetMapping("/drug/list")
  public Result<?> drugs() {
    requireAdmin();
    return Result.success(adminCatalogService.drugs());
  }

  @PostMapping("/drug/save")
  public Result<?> saveDrug(@Valid @RequestBody DrugSaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.saveDrug(request));
  }

  @GetMapping("/prompt-template/list")
  public Result<?> prompts() {
    requireAdmin();
    return Result.success(adminCatalogService.prompts());
  }

  @PostMapping("/prompt-template/save")
  public Result<?> savePrompt(@Valid @RequestBody PromptTemplateSaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.savePrompt(request));
  }

  @GetMapping("/knowledge/list")
  public Result<?> knowledgeEntries() {
    requireAdmin();
    return Result.success(adminCatalogService.knowledgeEntries());
  }

  @PostMapping("/knowledge/save")
  public Result<?> saveKnowledgeEntry(@Valid @RequestBody KnowledgeEntrySaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.saveKnowledgeEntry(request));
  }

  private void requireAdmin() {
    currentUserService.require(RoleType.ADMIN);
  }
}


