package com.smartcloudbrain.diagnosis.security;

import com.smartcloudbrain.common.security.RoleType;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  public String issueToken(Long userId, RoleType role) {
    return "mock-token-" + role.name().toLowerCase() + "-" + userId;
  }
}
