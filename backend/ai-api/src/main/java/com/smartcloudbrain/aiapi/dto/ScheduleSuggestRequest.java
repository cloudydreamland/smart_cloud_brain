package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record ScheduleSuggestRequest(
    @NotNull LocalDate startDate,
    @Min(1) @Max(14) int days,
    @NotNull List<ScheduleDoctorCandidate> doctors,
    @NotNull List<ScheduleDepartmentCandidate> departments,
    @NotNull List<ExistingSchedule> existingSchedules
) {
}
