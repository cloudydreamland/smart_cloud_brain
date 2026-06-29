package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotNull;

public record PatientNoticeStatusRequest(
    @NotNull Long id,
    String status
) {}
