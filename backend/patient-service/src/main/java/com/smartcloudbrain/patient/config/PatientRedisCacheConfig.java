package com.smartcloudbrain.patient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
public class PatientRedisCacheConfig implements CachingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(PatientRedisCacheConfig.class);

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .disableCachingNullValues()
        .prefixCacheNameWith("scb:")
        .serializeValuesWith(SerializationPair.fromSerializer(serializer));

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(defaults)
        .withInitialCacheConfigurations(Map.of(
            "patient:department:list", defaults.entryTtl(Duration.ofMinutes(30)),
            "patient:doctor:list", defaults.entryTtl(Duration.ofMinutes(10))
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
