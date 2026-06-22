package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record ScheduleSuggestResponse(
    List<ScheduleSuggestionItem> suggestions,
    String provider,
    String model,
    boolean degraded
) {
  public ScheduleSuggestResponse(List<ScheduleSuggestionItem> suggestions, String provider, boolean degraded) {
    this(suggestions, provider, "", degraded);
  }

  public ScheduleSuggestResponse withRuntime(String provider, String model) {
    return new ScheduleSuggestResponse(suggestions, provider, model, degraded);
  }
}
