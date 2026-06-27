package com.smartcloudbrain.notification.dto;

public record NotificationCreateRequest(
    Long doctorId,
    Long patientId,
    Long prescriptionId,
    String type,
    String title,
    String content,
    String riskLevel,
    Long triageRecordId,
    Long medicalRecordId
) {
  public NotificationCreateRequest(
      Long doctorId,
      Long patientId,
      Long prescriptionId,
      String type,
      String title,
      String content,
      String riskLevel
  ) {
    this(doctorId, patientId, prescriptionId, type, title, content, riskLevel, null, null);
  }
}
