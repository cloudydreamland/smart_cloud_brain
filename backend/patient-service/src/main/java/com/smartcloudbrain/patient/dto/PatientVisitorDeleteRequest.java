package com.smartcloudbrain.patient.dto;

import jakarta.validation.constraints.NotNull;

public record PatientVisitorDeleteRequest(@NotNull Long id) {
}
