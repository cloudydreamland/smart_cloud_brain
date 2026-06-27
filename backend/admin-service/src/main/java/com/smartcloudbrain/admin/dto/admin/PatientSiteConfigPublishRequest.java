package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record PatientSiteConfigPublishRequest(
    @NotBlank String configKey,
    @NotBlank String remark
) {
}
