package com.smartcloudbrain.aiapi.dto;

public record MedicalRecordGenerateResponse(
    String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String physicalExam,
    String diagnosis,
    String treatmentAdvice,
    String soapContent,
    boolean degraded,
    String provider,
    String model
) {
  public MedicalRecordGenerateResponse(
      String chiefComplaint,
      String presentIllness,
      String pastHistory,
      String physicalExam,
      String diagnosis,
      String treatmentAdvice,
      String soapContent,
      boolean degraded
  ) {
    this(chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis, treatmentAdvice, soapContent, degraded, "", "");
  }

  public MedicalRecordGenerateResponse(
      String chiefComplaint,
      String presentIllness,
      String pastHistory,
      String physicalExam,
      String diagnosis,
      String treatmentAdvice,
      boolean degraded
  ) {
    this(chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis, treatmentAdvice, "", degraded, "", "");
  }

  public MedicalRecordGenerateResponse withRuntime(String provider, String model) {
    return new MedicalRecordGenerateResponse(chiefComplaint, presentIllness, pastHistory, physicalExam, diagnosis,
        treatmentAdvice, soapContent, degraded, provider, model);
  }
}
