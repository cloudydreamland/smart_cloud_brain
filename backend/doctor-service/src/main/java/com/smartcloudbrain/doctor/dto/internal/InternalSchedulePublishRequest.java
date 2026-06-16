package com.smartcloudbrain.doctor.dto.internal;

import java.time.LocalDate;
import java.util.List;

public record InternalSchedulePublishRequest(
    List<ScheduleItem> schedules
) {
  public record ScheduleItem(
      Long doctorId,
      Long departmentId,
      LocalDate workDate,
      String timeRange,
      Integer capacity
  ) {
  }
}
