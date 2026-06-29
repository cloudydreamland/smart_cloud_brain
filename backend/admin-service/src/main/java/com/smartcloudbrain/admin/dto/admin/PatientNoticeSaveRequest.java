package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record PatientNoticeSaveRequest(
    Long id,
    @NotBlank @Size(max = 120) String title,
    @NotBlank String content,
    @Size(max = 32) String linkType,
    @Size(max = 500) String linkUrl,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Boolean pinned,
    Integer sort,
    @Size(max = 20) String status
) {}
