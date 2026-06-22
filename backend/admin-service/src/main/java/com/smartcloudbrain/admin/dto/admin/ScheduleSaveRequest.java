package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleSaveRequest(
    Long id,
    @NotNull Long doctorId,
    @NotNull Long departmentId,
    @NotNull LocalDate workDate,
    @NotBlank String timeRange,
    @NotNull Integer capacity,
    String status
) {
}
