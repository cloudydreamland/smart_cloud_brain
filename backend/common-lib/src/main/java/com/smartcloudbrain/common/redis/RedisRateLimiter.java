package com.smartcloudbrain.common.redis;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisRateLimiter {

  private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

  private final StringRedisTemplate redisTemplate;

  public RedisRateLimiter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean allow(String key, int maxAttempts, Duration window) {
    if (key == null || key.isBlank() || maxAttempts <= 0 || window == null || window.isNegative() || window.isZero()) {
      return true;
    }
    try {
      Long attempts = redisTemplate.opsForValue().increment(key);
      if (attempts != null && attempts == 1L) {
        redisTemplate.expire(key, window);
      }
      return attempts == null || attempts <= maxAttempts;
    } catch (RuntimeException ex) {
      log.warn("Redis rate limiter failed for key={}: {}", key, ex.getMessage());
      return true;
    }
  }
}
