package com.smartcloudbrain.aiapi.dto;

public record MedicalRecordGenerateResponse(
    String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String physicalExam,
    String diagnosis,
    String treatmentAdvice,
    boolean degraded
) {
}
