package com.smartcloudbrain.admin.dto.admin;

import java.util.Map;

public record AssetUploadPolicyResponse(
    String provider,
    String bucket,
    String objectKey,
    String uploadMethod,
    String uploadUrl,
    Map<String, String> headers,
    Map<String, String> formData,
    String publicUrl,
    long expiresAt
) {}
