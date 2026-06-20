package com.smartcloudbrain.aiapi.dto;

import java.util.List;

public record ScheduleSuggestResponse(
    List<ScheduleSuggestionItem> suggestions,
    String provider,
    boolean degraded
) {
}
