package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record TriageRequest(
    Long patientId,
    @NotBlank String chiefComplaint,
    String symptoms,
    Integer age,
    String gender,
    String allergyHistory,
    String pastHistory
) {
  public TriageRequest(Long patientId, String chiefComplaint) {
    this(patientId, chiefComplaint, "", null, "", "", "");
  }
}
