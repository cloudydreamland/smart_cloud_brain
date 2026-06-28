package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyRequest;
import com.smartcloudbrain.admin.service.AdminPermissionGuard;
import com.smartcloudbrain.admin.service.storage.AssetUploadPolicyService;
import com.smartcloudbrain.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAssetController {

  private final AdminPermissionGuard adminPermissionGuard;
  private final AssetUploadPolicyService assetUploadPolicyService;

  public AdminAssetController(AdminPermissionGuard adminPermissionGuard, AssetUploadPolicyService assetUploadPolicyService) {
    this.adminPermissionGuard = adminPermissionGuard;
    this.assetUploadPolicyService = assetUploadPolicyService;
  }

  @PostMapping("/api/admin/assets/upload-policy")
  public Result<?> uploadPolicy(@Valid @RequestBody AssetUploadPolicyRequest request) {
    adminPermissionGuard.requirePermission("patient-site:manage");
    return Result.success(assetUploadPolicyService.createUploadPolicy(request));
  }
}
