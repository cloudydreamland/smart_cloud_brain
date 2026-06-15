package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record SystemDictSaveRequest(
    Long id,
    @NotBlank String dictType,
    @NotBlank String dictKey,
    @NotBlank String dictValue,
    Integer sort,
    String status
) {
}
