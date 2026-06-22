package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceStatusRequest(
    @NotNull Long deviceId,
    @NotBlank String status
) {
}
