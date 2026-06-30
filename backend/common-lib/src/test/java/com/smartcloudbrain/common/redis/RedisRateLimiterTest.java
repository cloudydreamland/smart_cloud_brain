package com.smartcloudbrain.common.redis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRateLimiterTest {

  @Test
  void allowsBeforeLimitAndSetsExpiryOnFirstHit() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> operations = mockOperations(redisTemplate);
    when(operations.increment("rate:test")).thenReturn(1L);

    RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

    assertTrue(limiter.allow("rate:test", 2, Duration.ofMinutes(1)));
    verify(redisTemplate).expire("rate:test", Duration.ofMinutes(1));
  }

  @Test
  void rejectsAfterLimit() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> operations = mockOperations(redisTemplate);
    when(operations.increment("rate:test")).thenReturn(3L);

    RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

    assertFalse(limiter.allow("rate:test", 2, Duration.ofMinutes(1)));
  }

  @Test
  void allowsWhenRedisFails() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    doThrow(new IllegalStateException("redis down")).when(redisTemplate).opsForValue();

    RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate);

    assertTrue(limiter.allow("rate:test", 2, Duration.ofMinutes(1)));
  }

  @SuppressWarnings("unchecked")
  private static ValueOperations<String, String> mockOperations(StringRedisTemplate redisTemplate) {
    ValueOperations<String, String> operations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(operations);
    return operations;
  }
}
