package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigPublishRequest;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigSaveRequest;
import com.smartcloudbrain.admin.service.AdminOperationsService;
import com.smartcloudbrain.admin.service.PatientSiteConfigService;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PatientSiteConfigController {

  private final PatientSiteConfigService patientSiteConfigService;
  private final AdminOperationsService adminOperationsService;
  private final CurrentUserService currentUserService;

  public PatientSiteConfigController(
      PatientSiteConfigService patientSiteConfigService,
      AdminOperationsService adminOperationsService,
      CurrentUserService currentUserService
  ) {
    this.patientSiteConfigService = patientSiteConfigService;
    this.adminOperationsService = adminOperationsService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/api/admin/patient-site/config")
  public Result<?> adminConfig(@RequestParam(name = "configKey", required = false) String configKey) {
    requireManagePermission();
    return Result.success(patientSiteConfigService.latest(configKey));
  }

  @GetMapping("/api/admin/patient-site/history")
  public Result<?> history(@RequestParam("configKey") String configKey) {
    requireManagePermission();
    return Result.success(patientSiteConfigService.history(configKey));
  }

  @PostMapping("/api/admin/patient-site/save")
  public Result<?> save(@Valid @RequestBody PatientSiteConfigSaveRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientSiteConfigService.saveDraft(request, user.userId()));
  }

  @PostMapping("/api/admin/patient-site/save-published")
  public Result<?> savePublished(@Valid @RequestBody PatientSiteConfigSaveRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientSiteConfigService.savePublished(request, user.userId()));
  }

  @PostMapping("/api/admin/patient-site/publish")
  public Result<?> publish(@Valid @RequestBody PatientSiteConfigPublishRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientSiteConfigService.publish(request.configKey(), request.remark(), user.userId()));
  }

  @PostMapping("/api/admin/patient-site/preview-token")
  public Result<?> previewToken(
      @RequestParam("configKey") String configKey,
      @RequestParam(name = "version", required = false) Integer version
  ) {
    requireManagePermission();
    return Result.success(patientSiteConfigService.createPreviewToken(configKey, version));
  }

  @GetMapping("/api/patient-site/config")
  public Result<?> publicConfig() {
    return Result.success(patientSiteConfigService.publicConfig());
  }

  @GetMapping("/api/patient-site/preview")
  public Result<?> previewConfig(@RequestParam("token") String token) {
    return Result.success(patientSiteConfigService.previewConfig(token));
  }

  private AuthenticatedUser requireManagePermission() {
    AuthenticatedUser user = currentUserService.require(RoleType.ADMIN);
    if (!adminOperationsService.hasPermission(user.role(), "patient-site:manage")) {
      throw new BusinessException(403, "Permission denied: patient-site:manage");
    }
    return user;
  }
}
