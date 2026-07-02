package com.smartcloudbrain.common.event;

public final class DomainEventNames {

  public static final String PRESCRIPTION_CHECKED = "prescription.checked";
  public static final String PRESCRIPTION_CREATED = "prescription.created";
  public static final String NOTIFICATION_CREATED = "notification.created";
  public static final String TRIAGE_ASSIGNED = "triage.assigned";
  public static final String REGISTRATION_CREATED = "registration.created";
  public static final String REGISTRATION_CANCELLED = "registration.cancelled";
  public static final String REGISTRATION_PAID = "registration.paid";
  public static final String REGISTRATION_CHECKED_IN = "registration.checked-in";
  public static final String REGISTRATION_WAITING = "registration.waiting";
  public static final String REGISTRATION_CALLED = "registration.called";
  public static final String REGISTRATION_STARTED = "registration.started";
  public static final String REGISTRATION_COMPLETED = "registration.completed";
  public static final String REGISTRATION_REFUNDED = "registration.refunded";
  public static final String SCHEDULE_PUBLISHED = "schedule.published";
  public static final String SCHEDULE_CANCELLED = "schedule.cancelled";
  public static final String AUTH_LOGIN = "auth.login";
  public static final String AUDIT_LOG = "audit.log";

  private DomainEventNames() {
  }
}
