package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AssetUploadPolicyRequest(
    @NotBlank String scene,
    @NotBlank String fileName,
    @NotBlank String contentType,
    long size
) {}
