package com.smartcloudbrain.common.redis;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisIdempotencyGuard {

  private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyGuard.class);

  private final StringRedisTemplate redisTemplate;

  public RedisIdempotencyGuard(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean acquire(String key, Duration ttl) {
    if (key == null || key.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
      return true;
    }
    try {
      Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
      return Boolean.TRUE.equals(acquired);
    } catch (RuntimeException ex) {
      log.warn("Redis idempotency guard failed for key={}, falling back to database consistency: {}", key, ex.getMessage());
      return true;
    }
  }

  public void release(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    try {
      redisTemplate.delete(key);
    } catch (RuntimeException ex) {
      log.warn("Redis idempotency guard release failed for key={}: {}", key, ex.getMessage());
    }
  }
}
