package com.smartcloudbrain.registration.service;

import java.util.Locale;
import java.util.Set;

public enum RegistrationStatus {
  PENDING_PAYMENT,
  PAID,
  CHECKED_IN,
  WAITING,
  CALLED,
  IN_CONSULTATION,
  COMPLETED,
  CANCELLED,
  NO_SHOW,
  REFUNDING,
  REFUNDED,
  CREATED,
  IN_PROGRESS;

  private static final Set<RegistrationStatus> NON_DUPLICATE_BLOCKING = Set.of(CANCELLED, REFUNDED);

  public static RegistrationStatus from(String value) {
    if (value == null || value.isBlank()) {
      return CREATED;
    }
    try {
      return RegistrationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return CREATED;
    }
  }

  public boolean canPay() {
    return this == PENDING_PAYMENT;
  }

  public boolean canCancel() {
    return this == PENDING_PAYMENT || this == PAID || this == CREATED;
  }

  public boolean canCheckIn() {
    return this == PAID || this == CREATED;
  }

  public boolean canJoinQueue() {
    return this == CHECKED_IN;
  }

  public boolean canCall() {
    return this == WAITING;
  }

  public boolean canStartConsultation() {
    return this == CALLED;
  }

  public boolean canComplete() {
    return this == IN_CONSULTATION || this == IN_PROGRESS;
  }

  public boolean canRefund() {
    return this == REFUNDING;
  }

  public boolean shouldBlockDuplicateSlot() {
    return !NON_DUPLICATE_BLOCKING.contains(this);
  }
}
