package com.smartcloudbrain.common.redis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisIdempotencyGuardTest {

  @Test
  void acquireSucceedsWhenKeyIsAbsent() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> operations = mockOperations(redisTemplate);
    when(operations.setIfAbsent("idem:test", "1", Duration.ofSeconds(60))).thenReturn(true);

    RedisIdempotencyGuard guard = new RedisIdempotencyGuard(redisTemplate);

    assertTrue(guard.acquire("idem:test", Duration.ofSeconds(60)));
  }

  @Test
  void acquireFailsWhenKeyAlreadyExists() {
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> operations = mockOperations(redisTemplate);
    when(operations.setIfAbsent("idem:test", "1", Duration.ofSeconds(60))).thenReturn(false);

    RedisIdempotencyGuard guard = new RedisIdempotencyGuard(redisTemplate);

    assertFalse(guard.acquire("idem:test", Duration.ofSeconds(60)));
  }

  @SuppressWarnings("unchecked")
  private static ValueOperations<String, String> mockOperations(StringRedisTemplate redisTemplate) {
    ValueOperations<String, String> operations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(operations);
    return operations;
  }
}
