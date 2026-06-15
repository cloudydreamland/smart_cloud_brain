package com.smartcloudbrain.common.event;

import java.time.Instant;

public record DomainEventEnvelope(
    String eventId,
    String eventType,
    Instant occurredAt,
    String source,
    String traceId,
    Object payload
) {
}
