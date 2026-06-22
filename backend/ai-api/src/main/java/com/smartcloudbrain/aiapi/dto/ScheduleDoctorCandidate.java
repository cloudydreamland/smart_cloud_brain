package com.smartcloudbrain.aiapi.dto;

public record ScheduleDoctorCandidate(
    Long doctorId,
    String doctorName,
    Long departmentId,
    String departmentCode,
    String specialty,
    boolean enabled
) {
}
