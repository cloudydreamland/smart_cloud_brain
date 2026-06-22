package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record PrescriptionCheckResponse(
    String riskLevel,
    String riskDescription,
    String suggestions,
    List<String> interactions,
    List<String> contraindications,
    List<String> adjustmentSuggestions,
    boolean degraded,
    String provider,
    String model
) {
  public PrescriptionCheckResponse(
      String riskLevel,
      String riskDescription,
      String suggestions,
      List<String> interactions,
      List<String> contraindications,
      List<String> adjustmentSuggestions,
      boolean degraded
  ) {
    this(riskLevel, riskDescription, suggestions, interactions, contraindications, adjustmentSuggestions, degraded, "", "");
  }

  public PrescriptionCheckResponse(
      String riskLevel,
      String suggestions,
      List<String> interactions,
      boolean degraded
  ) {
    this(riskLevel, "", suggestions, interactions, List.of(), List.of(), degraded, "", "");
  }

  public PrescriptionCheckResponse withRuntime(String provider, String model) {
    return new PrescriptionCheckResponse(riskLevel, riskDescription, suggestions, interactions, contraindications,
        adjustmentSuggestions, degraded, provider, model);
  }
}
