package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record TriageRequest(
    Long patientId,
    @NotBlank String chiefComplaint,
    String symptoms,
    Integer age,
    String gender,
    String allergyHistory,
    String pastHistory,
    String subjectType,
    Long subjectId
) {
  public TriageRequest(Long patientId, String chiefComplaint) {
    this(patientId, chiefComplaint, "", null, "", "", "", null, null);
  }

  public TriageRequest(
      Long patientId,
      String chiefComplaint,
      String symptoms,
      Integer age,
      String gender,
      String allergyHistory,
      String pastHistory
  ) {
    this(patientId, chiefComplaint, symptoms, age, gender, allergyHistory, pastHistory, null, null);
  }
}
