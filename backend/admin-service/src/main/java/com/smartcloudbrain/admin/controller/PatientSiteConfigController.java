package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigPublishRequest;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeSortRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeStatusRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSortRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationStatusRequest;
import com.smartcloudbrain.admin.service.AdminPermissionGuard;
import com.smartcloudbrain.admin.service.PatientNoticeService;
import com.smartcloudbrain.admin.service.PatientPortalContentService;
import com.smartcloudbrain.admin.service.PatientRecommendationService;
import com.smartcloudbrain.admin.service.PatientSiteConfigService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PatientSiteConfigController {

  private final PatientSiteConfigService patientSiteConfigService;
  private final PatientNoticeService patientNoticeService;
  private final PatientRecommendationService patientRecommendationService;
  private final PatientPortalContentService patientPortalContentService;
  private final AdminPermissionGuard adminPermissionGuard;

  public PatientSiteConfigController(
      PatientSiteConfigService patientSiteConfigService,
      PatientNoticeService patientNoticeService,
      PatientRecommendationService patientRecommendationService,
      PatientPortalContentService patientPortalContentService,
      AdminPermissionGuard adminPermissionGuard
  ) {
    this.patientSiteConfigService = patientSiteConfigService;
    this.patientNoticeService = patientNoticeService;
    this.patientRecommendationService = patientRecommendationService;
    this.patientPortalContentService = patientPortalContentService;
    this.adminPermissionGuard = adminPermissionGuard;
  }

  @GetMapping("/api/admin/patient-site/config")
  public Result<?> adminConfig(@RequestParam(name = "configKey", required = false) String configKey) {
    requireManagePermission();
    return Result.success(patientSiteConfigService.latest(configKey));
  }

  @GetMapping("/api/admin/patient-site/history")
  public Result<?> history(
      @RequestParam("configKey") String configKey,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
  ) {
    requireManagePermission();
    return Result.success(patientSiteConfigService.history(configKey, page, pageSize));
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

  @PostMapping("/api/admin/patient-site/site-preview-token")
  public Result<?> sitePreviewToken() {
    requireManagePermission();
    return Result.success(patientSiteConfigService.createSitePreviewToken());
  }

  @GetMapping("/api/patient-site/config")
  public Result<?> publicConfig() {
    return Result.success(patientSiteConfigService.publicConfig());
  }

  @GetMapping("/api/patient-site/home")
  public Result<?> publicHome() {
    return Result.success(patientPortalContentService.home());
  }

  @GetMapping("/api/patient-site/nav")
  public Result<?> publicNav() {
    return Result.success(patientSiteConfigService.publicConfig().get("nav"));
  }

  @GetMapping("/api/patient-site/hospital-info")
  public Result<?> publicHospitalInfo() {
    return Result.success(patientSiteConfigService.publicConfig().get("hospitalInfo"));
  }

  @GetMapping("/api/patient-site/notices")
  public Result<?> publicNotices() {
    return Result.success(patientNoticeService.publicList());
  }

  @GetMapping("/api/patient-site/recommendations")
  public Result<?> publicRecommendations(@RequestParam("type") String type) {
    return Result.success(patientRecommendationService.publicList(type));
  }

  @GetMapping("/api/patient-site/preview")
  public Result<?> previewConfig(@RequestParam("token") String token) {
    return Result.success(patientSiteConfigService.previewConfig(token));
  }

  @GetMapping("/api/admin/patient-site/notices")
  public Result<?> adminNotices() {
    requireManagePermission();
    return Result.success(patientNoticeService.adminList());
  }

  @PostMapping("/api/admin/patient-site/notices/save")
  public Result<?> saveNotice(@Valid @RequestBody PatientNoticeSaveRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientNoticeService.save(request, user.userId()));
  }

  @PostMapping("/api/admin/patient-site/notices/status")
  public Result<?> noticeStatus(@Valid @RequestBody PatientNoticeStatusRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientNoticeService.updateStatus(request, user.userId()));
  }

  @PostMapping("/api/admin/patient-site/notices/delete")
  public Result<?> deleteNotice(@RequestBody Map<String, Long> request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientNoticeService.delete(request.get("id"), user.userId()));
  }

  @PostMapping("/api/admin/patient-site/notices/sort")
  public Result<?> sortNotices(@Valid @RequestBody PatientNoticeSortRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientNoticeService.sort(request, user.userId()));
  }

  @GetMapping("/api/admin/patient-site/recommendations")
  public Result<?> adminRecommendations(@RequestParam("type") String type) {
    requireManagePermission();
    return Result.success(patientRecommendationService.adminList(type));
  }

  @PostMapping("/api/admin/patient-site/recommendations/save")
  public Result<?> saveRecommendation(@Valid @RequestBody PatientRecommendationSaveRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientRecommendationService.save(request, user.userId()));
  }

  @PostMapping("/api/admin/patient-site/recommendations/status")
  public Result<?> recommendationStatus(@Valid @RequestBody PatientRecommendationStatusRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientRecommendationService.updateStatus(request, user.userId()));
  }

  @PostMapping("/api/admin/patient-site/recommendations/delete")
  public Result<?> deleteRecommendation(@RequestBody Map<String, Long> request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientRecommendationService.delete(request.get("id"), user.userId()));
  }

  @PostMapping("/api/admin/patient-site/recommendations/sort")
  public Result<?> sortRecommendations(@Valid @RequestBody PatientRecommendationSortRequest request) {
    AuthenticatedUser user = requireManagePermission();
    return Result.success(patientRecommendationService.sort(request, user.userId()));
  }

  private AuthenticatedUser requireManagePermission() {
    return adminPermissionGuard.requirePermission("patient-site:manage");
  }
}
