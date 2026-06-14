package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record TriageResponse(
    String recommendedDepartment,
    String departmentCode,
    List<Long> recommendedDoctorIds,
    String reason,
    boolean degraded
) {
}
