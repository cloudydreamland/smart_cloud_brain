package com.smartcloudbrain.diagnosis.dto.registration;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateRegistrationRequest(
    @NotNull Long doctorId,
    @NotNull Long departmentId,
    @NotNull LocalDateTime appointmentTime,
    Long triageRecordId
) {
}
