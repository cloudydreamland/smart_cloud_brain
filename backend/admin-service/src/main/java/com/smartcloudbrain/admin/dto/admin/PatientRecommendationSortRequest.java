package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PatientRecommendationSortRequest(
    @NotNull List<SortItem> items
) {
  public record SortItem(@NotNull Long id, @NotNull Integer sort) {}
}
