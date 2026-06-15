package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record DrugSaveRequest(
    Long id,
    @NotBlank String name,
    String specification,
    String contraindication,
    String interactionRule,
    String status
) {
}


