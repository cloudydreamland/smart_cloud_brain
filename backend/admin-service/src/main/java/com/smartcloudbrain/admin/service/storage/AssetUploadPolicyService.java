package com.smartcloudbrain.admin.service.storage;

import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyRequest;
import com.smartcloudbrain.admin.dto.admin.AssetUploadPolicyResponse;
import org.springframework.stereotype.Service;

@Service
public class AssetUploadPolicyService {

  private final ObjectStorageProviderFactory providerFactory;

  public AssetUploadPolicyService(ObjectStorageProviderFactory providerFactory) {
    this.providerFactory = providerFactory;
  }

  public AssetUploadPolicyResponse createUploadPolicy(AssetUploadPolicyRequest request) {
    return providerFactory.currentProvider().createUploadPolicy(request);
  }
}
