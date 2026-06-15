package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotNull;

public record TriageAssignRequest(
    @NotNull Long triageRecordId,
    @NotNull Long doctorId
) {
}
