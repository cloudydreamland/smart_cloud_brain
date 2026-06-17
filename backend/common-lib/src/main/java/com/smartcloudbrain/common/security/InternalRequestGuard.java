package com.smartcloudbrain.common.security;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InternalRequestGuard {

  public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private final HttpServletRequest request;
  private final String expectedToken;

  public InternalRequestGuard(
      HttpServletRequest request,
      @Value("${internal.service-token:${INTERNAL_SERVICE_TOKEN:smart-cloud-brain-internal-local-token-change}}") String expectedToken
  ) {
    this.request = request;
    this.expectedToken = expectedToken;
  }

  public void requireServiceRequest() {
    String actualToken = request.getHeader(INTERNAL_TOKEN_HEADER);
    if (isBlank(expectedToken) || isBlank(actualToken) || !constantTimeEquals(expectedToken, actualToken)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  private boolean constantTimeEquals(String expected, String actual) {
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expectedBytes, actualBytes);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
