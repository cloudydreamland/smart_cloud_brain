package com.smartcloudbrain.auth.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

  public String encode(String raw) {
    return "{sha256}" + sha256(raw);
  }

  public void assertMatches(String raw, String stored) {
    if (stored == null || raw == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    if (stored.startsWith("{sha256}") && stored.equals(encode(raw))) {
      return;
    }
    if (stored.startsWith("{plain}") && stored.equals("{plain}" + raw)) {
      return;
    }
    throw new BusinessException(ErrorCode.UNAUTHORIZED);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash password", ex);
    }
  }
}

