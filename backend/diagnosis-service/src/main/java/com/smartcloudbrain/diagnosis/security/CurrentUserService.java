package com.smartcloudbrain.diagnosis.security;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private static final String PREFIX = "Bearer mock-token-";

  private final HttpServletRequest request;

  public CurrentUserService(HttpServletRequest request) {
    this.request = request;
  }

  public CurrentUser get() {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith(PREFIX)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    String[] parts = authorization.substring(PREFIX.length()).split("-");
    if (parts.length != 2) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    try {
      RoleType role = RoleType.valueOf(parts[0].toUpperCase());
      Long userId = Long.valueOf(parts[1]);
      return new CurrentUser(userId, role, role.name() + " " + userId);
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }
}
