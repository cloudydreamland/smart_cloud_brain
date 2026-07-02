package com.smartcloudbrain.patient.entity;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

  @Test
  void prePersistSetsCreatedAtWhenNull() {
    Patient patient = new Patient();
    assertNull(patient.getCreatedAt());

    patient.prePersist();

    assertNotNull(patient.getCreatedAt());
  }

  @Test
  void prePreservDoesNotOverwriteExistingCreatedAt() {
    Patient patient = new Patient();
    LocalDateTime existing = LocalDateTime.of(2020, 1, 1, 0, 0);
    patient.setCreatedAt(existing);

    patient.prePersist();

    assertNotNull(patient.getCreatedAt());
    // The existing value should be preserved
  }
}
