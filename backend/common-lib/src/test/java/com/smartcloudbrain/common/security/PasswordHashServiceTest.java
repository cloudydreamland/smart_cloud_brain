package com.smartcloudbrain.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PasswordHashServiceTest {

  private final PasswordHashService service = new PasswordHashService();

  @Test
  void encodesAndMatchesBcryptPassword() {
    String encoded = service.encode("123456");

    assertTrue(encoded.startsWith("{bcrypt}"));
    assertTrue(service.matches("123456", encoded));
    assertFalse(service.matches("bad", encoded));
    assertFalse(service.requiresUpgrade(encoded));
  }

  @Test
  void supportsLegacyHashesAndDetectsUpgradeNeed() throws Exception {
    String sha256 = "{sha256}" + HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest("123456".getBytes(StandardCharsets.UTF_8)));

    assertTrue(service.matches("123456", sha256));
    assertTrue(service.matches("123456", "{plain}123456"));
    assertTrue(service.requiresUpgrade(sha256));
    assertTrue(service.requiresUpgrade("{plain}123456"));
    assertTrue(service.requiresUpgrade(null));
  }

  @Test
  void rejectsEmptyRawPassword() {
    assertThrows(BusinessException.class, () -> service.encode(""));
    assertFalse(service.matches(null, "{plain}123456"));
    assertFalse(service.matches("123456", null));
  }
}
