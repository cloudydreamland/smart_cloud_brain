package com.smartcloudbrain.auth.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.PasswordHashService;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

  private final PasswordHashService passwordHashService;

  public PasswordService(PasswordHashService passwordHashService) {
    this.passwordHashService = passwordHashService;
  }

  public String encode(String raw) {
    return passwordHashService.encode(raw);
  }

  public void assertMatches(String raw, String stored) {
    if (!passwordHashService.matches(raw, stored)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  public boolean requiresUpgrade(String stored) {
    return passwordHashService.requiresUpgrade(stored);
  }
}

