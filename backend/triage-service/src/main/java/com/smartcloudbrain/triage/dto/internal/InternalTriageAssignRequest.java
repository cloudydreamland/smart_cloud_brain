package com.smartcloudbrain.triage.dto.internal;

public record InternalTriageAssignRequest(
    Long triageRecordId,
    Long doctorId
) {
}
