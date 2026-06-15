package com.smartcloudbrain.common.security;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtService {

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final byte[] secret;
  private final Duration ttl;

  public JwtService(String secret, Duration ttl) {
    if (secret == null || secret.length() < 16) {
      throw new IllegalArgumentException("JWT secret must contain at least 16 characters");
    }
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.ttl = ttl;
  }

  public String issue(Long userId, RoleType role, String name) {
    long exp = Instant.now().plus(ttl).getEpochSecond();
    String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    String payload = encode("{\"sub\":\"%s\",\"role\":\"%s\",\"name\":\"%s\",\"exp\":%d}"
        .formatted(userId, role.name(), escape(name), exp));
    return header + "." + payload + "." + sign(header + "." + payload);
  }

  public JwtClaims verify(String token) {
    if (token == null || token.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    String[] parts = token.split("\\.");
    if (parts.length != 3 || !constantTimeEquals(sign(parts[0] + "." + parts[1]), parts[2])) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    String payload = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
    long exp = Long.parseLong(required(payload, "exp"));
    if (Instant.now().getEpochSecond() >= exp) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return new JwtClaims(
        Long.valueOf(required(payload, "sub")),
        RoleType.valueOf(required(payload, "role")),
        unescape(optional(payload, "name")),
        Instant.ofEpochSecond(exp)
    );
  }

  private String sign(String content) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to sign JWT", ex);
    }
  }

  private static String encode(String value) {
    return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String required(String json, String field) {
    String value = optional(json, field);
    if (value == null || value.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return value;
  }

  private static String optional(String json, String field) {
    String quoted = "\"" + field + "\":\"";
    int quotedStart = json.indexOf(quoted);
    if (quotedStart >= 0) {
      int valueStart = quotedStart + quoted.length();
      int valueEnd = json.indexOf('"', valueStart);
      return valueEnd < 0 ? "" : json.substring(valueStart, valueEnd);
    }
    String numeric = "\"" + field + "\":";
    int numericStart = json.indexOf(numeric);
    if (numericStart >= 0) {
      int valueStart = numericStart + numeric.length();
      int valueEnd = valueStart;
      while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
        valueEnd++;
      }
      return json.substring(valueStart, valueEnd);
    }
    return null;
  }

  private static String escape(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String unescape(String value) {
    return value == null ? "" : value.replace("\\\"", "\"").replace("\\\\", "\\");
  }

  private static boolean constantTimeEquals(String expected, String actual) {
    if (expected == null || actual == null || expected.length() != actual.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < expected.length(); i++) {
      result |= expected.charAt(i) ^ actual.charAt(i);
    }
    return result == 0;
  }
}
