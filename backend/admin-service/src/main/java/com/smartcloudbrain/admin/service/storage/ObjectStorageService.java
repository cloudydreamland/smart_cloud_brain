package com.smartcloudbrain.admin.service.storage;

import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyRequest;
import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyResponse;

public interface ObjectStorageService {
  AssetUploadPolicyResponse createUploadPolicy(AssetUploadPolicyRequest request);
}
