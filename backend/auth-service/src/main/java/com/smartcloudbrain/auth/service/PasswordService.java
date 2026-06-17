package com.smartcloudbrain.auth.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

  private static final String BCRYPT_PREFIX = "{bcrypt}";
  private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

  public String encode(String raw) {
    return BCRYPT_PREFIX + bcrypt.encode(raw);
  }

  public void assertMatches(String raw, String stored) {
    if (stored == null || raw == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    if (stored.startsWith(BCRYPT_PREFIX) && bcrypt.matches(raw, stored.substring(BCRYPT_PREFIX.length()))) {
      return;
    }
    if (stored.startsWith("{sha256}") && stored.equals("{sha256}" + sha256(raw))) {
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

