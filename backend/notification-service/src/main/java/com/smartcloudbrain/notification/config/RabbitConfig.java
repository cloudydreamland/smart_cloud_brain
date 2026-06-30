package com.smartcloudbrain.notification.config;

import com.smartcloudbrain.common.event.RabbitTopology;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitConfig {

  @Bean
  TopicExchange domainExchange() {
    return new TopicExchange(RabbitTopology.DOMAIN_EXCHANGE, true, false);
  }

  @Bean
  TopicExchange deadExchange() {
    return new TopicExchange(RabbitTopology.DEAD_EXCHANGE, true, false);
  }

  @Bean
  Queue notificationDispatchQueue() {
    return new Queue(RabbitTopology.NOTIFICATION_DISPATCH_QUEUE, true, false, false, Map.of(
        "x-dead-letter-exchange", RabbitTopology.DEAD_EXCHANGE
    ));
  }

  @Bean
  Queue auditLogQueue() {
    return new Queue(RabbitTopology.AUDIT_LOG_QUEUE, true, false, false, Map.of(
        "x-dead-letter-exchange", RabbitTopology.DEAD_EXCHANGE
    ));
  }

  @Bean
  Queue deadLetterQueue() {
    return new Queue(RabbitTopology.DEAD_LETTER_QUEUE, true);
  }

  @Bean
  Binding notificationDispatchBinding(
      @Qualifier("notificationDispatchQueue") Queue notificationDispatchQueue,
      @Qualifier("domainExchange") TopicExchange domainExchange
  ) {
    return BindingBuilder.bind(notificationDispatchQueue)
        .to(domainExchange)
        .with(RabbitTopology.ROUTING_NOTIFICATION_DISPATCH);
  }

  @Bean
  Binding auditLogBinding(
      @Qualifier("auditLogQueue") Queue auditLogQueue,
      @Qualifier("domainExchange") TopicExchange domainExchange
  ) {
    return BindingBuilder.bind(auditLogQueue)
        .to(domainExchange)
        .with(RabbitTopology.ROUTING_AUDIT_LOG);
  }

  @Bean
  Binding deadLetterBinding(
      @Qualifier("deadLetterQueue") Queue deadLetterQueue,
      @Qualifier("deadExchange") TopicExchange deadExchange
  ) {
    return BindingBuilder.bind(deadLetterQueue).to(deadExchange).with("#");
  }
}
