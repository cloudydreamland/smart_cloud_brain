package com.smartcloudbrain.doctor.dto.internal;

import java.time.LocalDate;

public record InternalScheduleSaveRequest(
    Long id,
    Long doctorId,
    Long departmentId,
    LocalDate workDate,
    String timeRange,
    Integer capacity,
    String status
) {
}
