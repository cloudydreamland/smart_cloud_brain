package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record PatientSiteConfigSaveRequest(
    @NotBlank String configKey,
    @NotBlank String configJson,
    String remark
) {
}
