package com.smartcloudbrain.aiapi.dto;

public record MedicalRecordGenerateResponse(
    String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String physicalExam,
    String diagnosis,
    String treatmentAdvice,
    String soapContent,
    boolean degraded
) {
  public MedicalRecordGenerateResponse(
      String chiefComplaint,
      String presentIllness,
      String pastHistory,
      String physicalExam,
      String diagnosis,
      String treatmentAdvice,
      boolean degraded
  ) {
    this(chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis, treatmentAdvice, "", degraded);
  }
}
