package com.smartcloudbrain.registration.service;

import java.util.Locale;

public enum PaymentStatus {
  UNPAID,
  PAID,
  CLOSED,
  REFUNDING,
  REFUNDED;

  public static PaymentStatus from(String value) {
    if (value == null || value.isBlank()) {
      return UNPAID;
    }
    try {
      return PaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return UNPAID;
    }
  }
}
