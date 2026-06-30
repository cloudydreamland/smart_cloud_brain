package com.smartcloudbrain.ai.config;

import com.smartcloudbrain.common.redis.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AiRedisConfig {

  @Bean
  public RedisRateLimiter redisRateLimiter(StringRedisTemplate redisTemplate) {
    return new RedisRateLimiter(redisTemplate);
  }
}
