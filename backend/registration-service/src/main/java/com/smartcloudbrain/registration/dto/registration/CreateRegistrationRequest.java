package com.smartcloudbrain.registration.dto.registration;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateRegistrationRequest(
    @NotNull Long doctorId,
    @NotNull Long departmentId,
    @NotNull LocalDateTime appointmentTime,
    Long triageRecordId,
    Long slotId,
    Long visitorId,
    String visitorType,
    String subjectType,
    Long subjectId
) {
  public CreateRegistrationRequest(
      Long doctorId,
      Long departmentId,
      LocalDateTime appointmentTime,
      Long triageRecordId,
      Long slotId
  ) {
    this(doctorId, departmentId, appointmentTime, triageRecordId, slotId, null, null, null, null);
  }

  public CreateRegistrationRequest(
      Long doctorId,
      Long departmentId,
      LocalDateTime appointmentTime,
      Long triageRecordId,
      Long slotId,
      Long visitorId,
      String visitorType
  ) {
    this(doctorId, departmentId, appointmentTime, triageRecordId, slotId, visitorId, visitorType, null, null);
  }
}


