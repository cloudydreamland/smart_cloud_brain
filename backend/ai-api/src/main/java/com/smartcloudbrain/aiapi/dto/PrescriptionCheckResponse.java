package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record PrescriptionCheckResponse(
    String riskLevel,
    String suggestions,
    List<String> interactions,
    boolean degraded
) {
}
