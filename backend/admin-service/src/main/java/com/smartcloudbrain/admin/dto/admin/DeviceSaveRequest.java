package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record DeviceSaveRequest(
    Long id,
    @NotBlank String deviceCode,
    @NotBlank String name,
    String category,
    Long departmentId,
    String location,
    String status,
    LocalDate purchaseDate,
    String remark
) {
}
