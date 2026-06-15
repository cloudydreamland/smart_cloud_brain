package com.smartcloudbrain.auth.config;

import com.smartcloudbrain.common.security.JwtService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

  @Bean
  JwtService jwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expire-hours}") long expireHours
  ) {
    return new JwtService(secret, Duration.ofHours(expireHours));
  }
}

