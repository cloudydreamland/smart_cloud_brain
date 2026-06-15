package com.smartcloudbrain.prescription.config;

import com.smartcloudbrain.common.event.RabbitTopology;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  @Bean
  TopicExchange domainExchange() {
    return new TopicExchange(RabbitTopology.DOMAIN_EXCHANGE, true, false);
  }
}
