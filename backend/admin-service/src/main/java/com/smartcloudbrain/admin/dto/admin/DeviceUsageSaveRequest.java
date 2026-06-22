package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record DeviceUsageSaveRequest(
    Long id,
    @NotNull Long deviceId,
    @NotBlank String usageType,
    String usedBy,
    Long patientId,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    String resultStatus,
    String remark
) {
}
