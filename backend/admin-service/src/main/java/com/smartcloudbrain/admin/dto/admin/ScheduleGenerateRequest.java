package com.smartcloudbrain.admin.dto.admin;

import java.time.LocalDate;

public record ScheduleGenerateRequest(
    LocalDate startDate,
    Integer days
) {
}
