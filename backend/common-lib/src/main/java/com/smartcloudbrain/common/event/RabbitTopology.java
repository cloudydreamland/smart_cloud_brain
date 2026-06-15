package com.smartcloudbrain.common.event;

public final class RabbitTopology {

  public static final String DOMAIN_EXCHANGE = "scb.domain";
  public static final String DEAD_EXCHANGE = "scb.dead";
  public static final String DEAD_LETTER_QUEUE = "scb.dead.letter";

  public static final String NOTIFICATION_DISPATCH_QUEUE = "scb.notification.dispatch";
  public static final String AUDIT_LOG_QUEUE = "scb.audit.log";

  public static final String ROUTING_NOTIFICATION_DISPATCH = "notification.dispatch";
  public static final String ROUTING_AUDIT_LOG = "audit.log";

  private RabbitTopology() {
  }
}
