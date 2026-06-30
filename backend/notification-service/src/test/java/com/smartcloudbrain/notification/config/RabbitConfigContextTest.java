package com.smartcloudbrain.notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartcloudbrain.common.event.RabbitTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class RabbitConfigContextTest {

  @Test
  void loadsAllRabbitBindingsWithoutAmbiguousQueueInjection() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RabbitConfig.class)) {
      assertEquals(3, context.getBeansOfType(Binding.class).size());
      assertEquals(RabbitTopology.NOTIFICATION_DISPATCH_QUEUE,
          context.getBean("notificationDispatchQueue", Queue.class).getName());
      assertEquals(RabbitTopology.AUDIT_LOG_QUEUE,
          context.getBean("auditLogQueue", Queue.class).getName());
    }
  }
}
