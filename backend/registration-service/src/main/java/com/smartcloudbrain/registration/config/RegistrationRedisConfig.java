package com.smartcloudbrain.registration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.common.redis.RedisIdempotencyGuard;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
public class RegistrationRedisConfig implements CachingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(RegistrationRedisConfig.class);

  @Bean
  public RedisRateLimiter redisRateLimiter(StringRedisTemplate redisTemplate) {
    return new RedisRateLimiter(redisTemplate);
  }

  @Bean
  public RedisIdempotencyGuard redisIdempotencyGuard(StringRedisTemplate redisTemplate) {
    return new RedisIdempotencyGuard(redisTemplate);
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(30))
        .disableCachingNullValues()
        .prefixCacheNameWith("scb:")
        .serializeValuesWith(SerializationPair.fromSerializer(serializer));

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(defaults)
        .withInitialCacheConfigurations(Map.of(
            "registration:slots", defaults.entryTtl(Duration.ofSeconds(30))
        ))
        .transactionAware()
        .build();
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new LoggingCacheErrorHandler(log);
  }

  private record LoggingCacheErrorHandler(Logger log) implements CacheErrorHandler {
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
      log.warn("Redis cache get failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
      log.warn("Redis cache put failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
      log.warn("Redis cache evict failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
      log.warn("Redis cache clear failed for cache={}: {}", cache.getName(), exception.getMessage());
    }
  }
}
