package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record PrescriptionCheckResponse(
    String riskLevel,
    String riskDescription,
    String suggestions,
    List<String> interactions,
    List<String> contraindications,
    List<String> adjustmentSuggestions,
    boolean degraded
) {
  public PrescriptionCheckResponse(
      String riskLevel,
      String suggestions,
      List<String> interactions,
      boolean degraded
  ) {
    this(riskLevel, "", suggestions, interactions, List.of(), List.of(), degraded);
  }
}
