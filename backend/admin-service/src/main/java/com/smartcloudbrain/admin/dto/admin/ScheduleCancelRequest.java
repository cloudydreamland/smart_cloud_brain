package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotNull;

public record ScheduleCancelRequest(
    @NotNull Long scheduleId
) {
}
