package com.smartcloudbrain.common.security;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private final HttpServletRequest request;

  public CurrentUserService(HttpServletRequest request) {
    this.request = request;
  }

  public AuthenticatedUser get() {
    String userId = request.getHeader(UserContextHeaders.USER_ID);
    String role = request.getHeader(UserContextHeaders.USER_ROLE);
    if (userId == null || role == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    try {
      String name = request.getHeader(UserContextHeaders.USER_NAME);
      return new AuthenticatedUser(
          Long.valueOf(userId),
          RoleType.valueOf(role),
          name == null ? "" : URLDecoder.decode(name, StandardCharsets.UTF_8)
      );
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  public AuthenticatedUser require(RoleType role) {
    AuthenticatedUser user = get();
    if (user.role() != role) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return user;
  }
}
