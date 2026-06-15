package com.smartcloudbrain.diagnosis.dto.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MedicalRecordSaveRequest(
    @NotNull Long registrationId,
    @NotBlank String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String physicalExam,
    @NotBlank String diagnosis,
    String treatmentAdvice,
    Boolean aiGenerated
) {
}
