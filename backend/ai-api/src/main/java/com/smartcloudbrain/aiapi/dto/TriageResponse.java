package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record TriageResponse(
    String recommendedDepartment,
    String departmentCode,
    String recommendedDoctorDirection,
    String urgencyLevel,
    Double confidence,
    List<Long> recommendedDoctorIds,
    String reason,
    boolean degraded,
    String provider,
    String model
) {
  public TriageResponse(
      String recommendedDepartment,
      String departmentCode,
      String recommendedDoctorDirection,
      String urgencyLevel,
      Double confidence,
      List<Long> recommendedDoctorIds,
      String reason,
      boolean degraded
  ) {
    this(recommendedDepartment, departmentCode, recommendedDoctorDirection, urgencyLevel, confidence,
        recommendedDoctorIds, reason, degraded, "", "");
  }

  public TriageResponse(
      String recommendedDepartment,
      String departmentCode,
      List<Long> recommendedDoctorIds,
      String reason,
      boolean degraded
  ) {
    this(recommendedDepartment, departmentCode, "", "", null, recommendedDoctorIds, reason, degraded, "", "");
  }

  public TriageResponse withRuntime(String provider, String model) {
    return new TriageResponse(recommendedDepartment, departmentCode, recommendedDoctorDirection, urgencyLevel,
        confidence, recommendedDoctorIds, reason, degraded, provider, model);
  }
}
