package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.admin.dto.admin.AccountSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceStatusRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceUsageSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.KnowledgeEntrySaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PromptTemplateSaveRequest;
import com.smartcloudbrain.admin.dto.admin.RolePermissionSaveRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleCancelRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleGenerateRequest;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleSaveRequest;
import com.smartcloudbrain.admin.dto.admin.SystemDictSaveRequest;
import com.smartcloudbrain.admin.dto.admin.TriageAssignRequest;
import com.smartcloudbrain.admin.service.AdminCatalogService;
import com.smartcloudbrain.admin.service.AdminOperationsService;
import com.smartcloudbrain.aiapi.dto.PromptTestRequest;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
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
  private final AdminOperationsService adminOperationsService;
  private final CurrentUserService currentUserService;

  public AdminController(AdminCatalogService adminCatalogService, AdminOperationsService adminOperationsService, CurrentUserService currentUserService) {
    this.adminCatalogService = adminCatalogService;
    this.adminOperationsService = adminOperationsService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/department/list")
  public Result<?> departments() {
    requireAdmin();
    return Result.success(adminCatalogService.departments());
  }

  @GetMapping("/doctor/list")
  public Result<?> doctors(@RequestParam(name = "departmentId", required = false) Long departmentId) {
    requireAdmin();
    return Result.success(adminCatalogService.doctors(departmentId));
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
  public Result<?> schedules(
      @RequestParam(name = "startDate", required = false) String startDate,
      @RequestParam(name = "endDate", required = false) String endDate,
      @RequestParam(name = "departmentId", required = false) Long departmentId,
      @RequestParam(name = "doctorId", required = false) Long doctorId,
      @RequestParam(name = "status", required = false) String status
  ) {
    requireAdminPermission("schedule:manage");
    return Result.success(adminOperationsService.schedules(startDate, endDate, departmentId, doctorId, status));
  }

  @PostMapping("/schedule/save")
  public Result<?> saveSchedule(@Valid @RequestBody ScheduleSaveRequest request) {
    requireAdminPermission("schedule:manage");
    return Result.success(adminOperationsService.saveSchedule(request));
  }

  @PostMapping("/schedule/cancel")
  public Result<?> cancelSchedule(@Valid @RequestBody ScheduleCancelRequest request) {
    requireAdminPermission("schedule:manage");
    return Result.success(adminOperationsService.cancelSchedule(request));
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

  @GetMapping("/device/list")
  public Result<?> devices(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "departmentId", required = false) Long departmentId,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "status", required = false) String status
  ) {
    requireAdminPermission("device:manage");
    return Result.success(adminOperationsService.devices(keyword, departmentId, category, status));
  }

  @PostMapping("/device/save")
  public Result<?> saveDevice(@Valid @RequestBody DeviceSaveRequest request) {
    requireAdminPermission("device:manage");
    return Result.success(adminOperationsService.saveDevice(request));
  }

  @PostMapping("/device/status")
  public Result<?> updateDeviceStatus(@Valid @RequestBody DeviceStatusRequest request) {
    requireAdminPermission("device:manage");
    return Result.success(adminOperationsService.updateDeviceStatus(request));
  }

  @GetMapping("/device/usage/list")
  public Result<?> deviceUsageList(@RequestParam(name = "deviceId", required = false) Long deviceId) {
    requireAdminPermission("device:manage");
    return Result.success(adminOperationsService.deviceUsages(deviceId));
  }

  @PostMapping("/device/usage/save")
  public Result<?> saveDeviceUsage(@Valid @RequestBody DeviceUsageSaveRequest request) {
    requireAdminPermission("device:manage");
    return Result.success(adminOperationsService.saveDeviceUsage(request));
  }

  @GetMapping("/patient/list")
  public Result<?> patients(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "gender", required = false) String gender,
      @RequestParam(name = "minAge", required = false) Integer minAge,
      @RequestParam(name = "maxAge", required = false) Integer maxAge
  ) {
    requireAdminPermission("patient:manage");
    return Result.success(adminOperationsService.patients(keyword, gender, minAge, maxAge));
  }

  @GetMapping("/patient/detail")
  public Result<?> patientDetail(@RequestParam("id") Long id) {
    requireAdminPermission("patient:manage");
    return Result.success(adminOperationsService.patientDetail(id));
  }

  @PostMapping("/patient/save")
  public Result<?> savePatient(@Valid @RequestBody PatientSaveRequest request) {
    requireAdminPermission("patient:manage");
    return Result.success(adminOperationsService.savePatient(request));
  }

  @GetMapping("/statistics/overview")
  public Result<?> statisticsOverview() {
    requireAdminPermission("statistics:view");
    return Result.success(adminOperationsService.overview());
  }

  @GetMapping("/dashboard/stats")
  public Result<?> dashboardStats() {
    requireAdminPermission("dashboard:view");
    return Result.success(adminOperationsService.overview());
  }

  @GetMapping("/statistics/trend")
  public Result<?> statisticsTrend(
      @RequestParam(name = "startDate", required = false) String startDate,
      @RequestParam(name = "endDate", required = false) String endDate
  ) {
    requireAdminPermission("statistics:view");
    return Result.success(adminOperationsService.trend(startDate, endDate));
  }

  @GetMapping("/statistics/doctor-workload")
  public Result<?> doctorWorkload(
      @RequestParam(name = "startDate", required = false) String startDate,
      @RequestParam(name = "endDate", required = false) String endDate
  ) {
    requireAdminPermission("statistics:view");
    return Result.success(adminOperationsService.doctorWorkload(startDate, endDate));
  }

  @GetMapping("/statistics/patient-distribution")
  public Result<?> patientDistribution() {
    requireAdminPermission("statistics:view");
    return Result.success(adminOperationsService.patientDistribution());
  }

  @GetMapping("/statistics/device-usage")
  public Result<?> deviceUsageStatistics() {
    requireAdminPermission("statistics:view");
    return Result.success(adminOperationsService.deviceUsage());
  }

  @GetMapping("/statistics/report")
  public Result<?> report(
      @RequestParam(name = "startDate", required = false) String startDate,
      @RequestParam(name = "endDate", required = false) String endDate
  ) {
    requireAdminPermission("statistics:export");
    return Result.success(adminOperationsService.report(startDate, endDate));
  }

  @GetMapping("/permission/list")
  public Result<?> permissions() {
    requireAdminPermission("permission:manage");
    return Result.success(adminOperationsService.permissions());
  }

  @GetMapping("/permission/my")
  public Result<?> myPermissions() {
    AuthenticatedUser user = currentUserService.require(RoleType.ADMIN);
    return Result.success(adminOperationsService.myPermissions(user.role()));
  }

  @PostMapping("/permission/save-role")
  public Result<?> saveRolePermissions(@Valid @RequestBody RolePermissionSaveRequest request) {
    requireAdminPermission("permission:manage");
    return Result.success(adminOperationsService.saveRolePermissions(request));
  }

  private void requireAdmin() {
    currentUserService.require(RoleType.ADMIN);
  }

  private void requireAdminPermission(String permissionKey) {
    AuthenticatedUser user = currentUserService.require(RoleType.ADMIN);
    if (!adminOperationsService.hasPermission(user.role(), permissionKey)) {
      throw new BusinessException(403, "Permission denied: " + permissionKey);
    }
  }
}


