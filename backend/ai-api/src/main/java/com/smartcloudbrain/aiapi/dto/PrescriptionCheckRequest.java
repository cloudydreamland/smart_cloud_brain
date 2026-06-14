package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PrescriptionCheckRequest(
    Long patientId,
    Long doctorId,
    @Valid @NotEmpty List<DrugItem> drugs
) {
}
