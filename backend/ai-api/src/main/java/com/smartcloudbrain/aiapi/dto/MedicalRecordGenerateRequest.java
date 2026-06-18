package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicalRecordGenerateRequest(
    @NotNull Long registrationId,
    String departmentCode,
    @NotBlank String dialogueText,
    Long patientId,
    String patientName,
    Integer patientAge,
    String patientGender,
    String allergyHistory,
    String pastHistory,
    Long doctorId,
    String doctorName,
    String appointmentTime
) {
  public MedicalRecordGenerateRequest(Long registrationId, String departmentCode, String dialogueText) {
    this(registrationId, departmentCode, dialogueText, null, "", null, "", "", "", null, "", "");
  }
}
