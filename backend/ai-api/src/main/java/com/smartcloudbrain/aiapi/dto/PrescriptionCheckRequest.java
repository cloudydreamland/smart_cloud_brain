package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PrescriptionCheckRequest(
    Long patientId,
    Long doctorId,
    Long medicalRecordId,
    String diagnosis,
    Integer patientAge,
    String patientGender,
    String allergyHistory,
    String pastHistory,
    @Valid @NotEmpty List<DrugItem> drugs
) {
  public PrescriptionCheckRequest(Long patientId, Long doctorId, List<DrugItem> drugs) {
    this(patientId, doctorId, null, "", null, "", "", "", drugs);
  }
}
