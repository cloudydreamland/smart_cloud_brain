package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.admin.dto.admin.AccountSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.KnowledgeEntrySaveRequest;
import com.smartcloudbrain.admin.dto.admin.PromptTemplateSaveRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleGenerateRequest;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.dto.admin.SystemDictSaveRequest;
import com.smartcloudbrain.admin.dto.admin.TriageAssignRequest;
import com.smartcloudbrain.admin.service.AdminCatalogService;
import com.smartcloudbrain.aiapi.dto.PromptTestRequest;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/account/list")
  public Result<?> accounts() {
    requireAdmin();
    return Result.success(adminCatalogService.accounts());
  }

  @GetMapping("/role/list")
  public Result<?> roles() {
    requireAdmin();
    return Result.success(adminCatalogService.roles());
  }

  @PostMapping("/account/save")
  public Result<?> saveAccount(@Valid @RequestBody AccountSaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.saveAccount(request));
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

  @PostMapping("/prompt-template/test")
  public Result<?> testPrompt(@Valid @RequestBody PromptTestRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.testPrompt(request));
  }

  @GetMapping("/ai-log/list")
  public Result<?> aiLogs() {
    requireAdmin();
    return Result.success(adminCatalogService.aiLogs());
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

  @GetMapping("/dict/list")
  public Result<?> dicts(@RequestParam(name = "dictType", required = false) String dictType) {
    requireAdmin();
    return Result.success(adminCatalogService.dicts(dictType));
  }

  @PostMapping("/dict/save")
  public Result<?> saveDict(@Valid @RequestBody SystemDictSaveRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.saveDict(request));
  }

  @PostMapping("/schedule/generate")
  public Result<?> generateSchedule(@RequestBody ScheduleGenerateRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.generateScheduleSuggestions(request));
  }

  @PostMapping("/schedule/publish")
  public Result<?> publishSchedule(@RequestBody SchedulePublishRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.publishSchedule(request));
  }

  @GetMapping("/schedule/list")
  public Result<?> schedules() {
    requireAdmin();
    return Result.success(adminCatalogService.schedules());
  }

  @GetMapping("/schedule/suggestion/detail")
  public Result<?> scheduleSuggestionDetail(@RequestParam("id") Long id) {
    requireAdmin();
    return Result.success(adminCatalogService.scheduleSuggestionDetail(id));
  }

  @GetMapping("/triage-desk/list")
  public Result<?> triageDesk() {
    requireAdmin();
    return Result.success(adminCatalogService.triageDesk());
  }

  @GetMapping("/triage-desk/detail")
  public Result<?> triageDetail(@RequestParam("id") Long id) {
    requireAdmin();
    return Result.success(adminCatalogService.triageDetail(id));
  }

  @PostMapping("/triage-desk/assign")
  public Result<?> assignTriage(@Valid @RequestBody TriageAssignRequest request) {
    requireAdmin();
    return Result.success(adminCatalogService.assignTriage(request.triageRecordId(), request.doctorId()));
  }

  @PostMapping("/triage-desk/close")
  public Result<?> closeTriage(@RequestBody java.util.Map<String, Long> request) {
    requireAdmin();
    return Result.success(adminCatalogService.closeTriage(request.get("triageRecordId")));
  }

  private void requireAdmin() {
    currentUserService.require(RoleType.ADMIN);
  }
}


