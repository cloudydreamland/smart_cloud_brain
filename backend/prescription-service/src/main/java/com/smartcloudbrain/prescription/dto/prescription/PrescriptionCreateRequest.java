package com.smartcloudbrain.prescription.dto.prescription;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PrescriptionCreateRequest(
    @NotNull Long patientId,
    @NotNull Long medicalRecordId,
    String riskLevel,
    @Valid @NotEmpty List<DrugItem> drugs
) {
}


