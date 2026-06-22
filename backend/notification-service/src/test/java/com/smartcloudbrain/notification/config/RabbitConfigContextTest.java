package com.smartcloudbrain.notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class RabbitConfigContextTest {

  @Test
  void loadsAllRabbitBindingsWithoutAmbiguousQueueInjection() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RabbitConfig.class)) {
      assertEquals(2, context.getBeansOfType(Binding.class).size());
    }
  }
}
