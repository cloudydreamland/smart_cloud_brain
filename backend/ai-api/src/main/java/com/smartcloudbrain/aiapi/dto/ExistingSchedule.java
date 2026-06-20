package com.smartcloudbrain.aiapi.dto;

import java.time.LocalDate;

public record ExistingSchedule(
    Long doctorId,
    LocalDate workDate,
    String timeRange
) {
}
