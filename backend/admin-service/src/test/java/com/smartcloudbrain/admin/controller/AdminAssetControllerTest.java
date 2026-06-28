package com.smartcloudbrain.admin.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyRequest;
import com.smartcloudbrain.admin.service.AdminPermissionGuard;
import com.smartcloudbrain.admin.service.storage.AssetUploadPolicyService;
import com.smartcloudbrain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAssetControllerTest {

  @Mock private AdminPermissionGuard adminPermissionGuard;
  @Mock private AssetUploadPolicyService assetUploadPolicyService;

  @Test
  void doesNotCreatePolicyWithoutPatientSitePermission() {
    AdminAssetController controller = new AdminAssetController(adminPermissionGuard, assetUploadPolicyService);
    AssetUploadPolicyRequest request = new AssetUploadPolicyRequest("patient-site", "logo.png", "image/png", 1024);
    when(adminPermissionGuard.requirePermission("patient-site:manage"))
        .thenThrow(new BusinessException(403, "Permission denied: patient-site:manage"));

    assertThrows(BusinessException.class, () -> controller.uploadPolicy(request));

    verify(assetUploadPolicyService, never()).createUploadPolicy(request);
  }
}
