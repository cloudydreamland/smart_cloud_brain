package com.smartcloudbrain.common.security;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

  private static final String BCRYPT_PREFIX = "{bcrypt}";
  private static final String SHA256_PREFIX = "{sha256}";
  private static final String PLAIN_PREFIX = "{plain}";
  private static final int BCRYPT_STRENGTH = 12;

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

  public String encode(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    return BCRYPT_PREFIX + encoder.encode(raw);
  }

  public boolean matches(String raw, String stored) {
    if (stored == null || raw == null) {
      return false;
    }
    if (stored.startsWith(BCRYPT_PREFIX)) {
      return encoder.matches(raw, stored.substring(BCRYPT_PREFIX.length()));
    }
    if (stored.startsWith(SHA256_PREFIX)) {
      return stored.equals(SHA256_PREFIX + sha256(raw));
    }
    if (stored.startsWith(PLAIN_PREFIX)) {
      return stored.equals(PLAIN_PREFIX + raw);
    }
    return false;
  }

  public boolean requiresUpgrade(String stored) {
    return stored == null || !stored.startsWith(BCRYPT_PREFIX);
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
