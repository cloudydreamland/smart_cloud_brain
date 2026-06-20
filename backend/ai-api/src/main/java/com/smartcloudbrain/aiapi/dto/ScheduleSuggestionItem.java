package com.smartcloudbrain.aiapi.dto;

import java.time.LocalDate;

public record ScheduleSuggestionItem(
    Long doctorId,
    Long departmentId,
    LocalDate workDate,
    String timeRange,
    Integer capacity,
    String reason
) {
}
