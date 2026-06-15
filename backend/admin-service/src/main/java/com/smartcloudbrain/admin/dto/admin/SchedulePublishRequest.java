package com.smartcloudbrain.admin.dto.admin;

import java.util.List;

public record SchedulePublishRequest(
    List<Long> suggestionIds
) {
}
