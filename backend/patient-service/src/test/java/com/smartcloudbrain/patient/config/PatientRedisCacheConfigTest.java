package com.smartcloudbrain.patient.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class PatientRedisCacheConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
      .withBean(ObjectMapper.class, ObjectMapper::new)
      .withUserConfiguration(PatientRedisCacheConfig.class);

  @Test
  void cacheManagerConfigurationLoads() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(CacheManager.class);
      assertThat(context.getBean(CacheManager.class).getCacheNames())
          .contains("patient:department:list", "patient:doctor:list");
    });
  }
}
