package com.smartcloudbrain.notification.dto;

public record NotificationCreateRequest(
    Long doctorId,
    Long patientId,
    Long prescriptionId,
    String type,
    String title,
    String content,
    String riskLevel
) {
}

