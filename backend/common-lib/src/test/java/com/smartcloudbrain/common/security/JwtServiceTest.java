package com.smartcloudbrain.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smartcloudbrain.common.exception.BusinessException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "0123456789abcdef";

  @Test
  void issuesAndVerifiesToken() {
    JwtService service = new JwtService(SECRET, Duration.ofMinutes(5));

    JwtClaims claims = service.verify(service.issue(7L, RoleType.DOCTOR, "doctor\\one"));

    assertEquals(7L, claims.userId());
    assertEquals(RoleType.DOCTOR, claims.role());
    assertEquals("doctor\\one", claims.name());
  }

  @Test
  void rejectsBlankTamperedAndExpiredTokens() {
    JwtService service = new JwtService(SECRET, Duration.ofMillis(1));
    String token = service.issue(1L, RoleType.PATIENT, "patient");
    String tampered = token.substring(0, token.length() - 2) + "xx";

    assertThrows(BusinessException.class, () -> service.verify(""));
    assertThrows(BusinessException.class, () -> service.verify(tampered));
    assertThrows(BusinessException.class, () ->
        new JwtService(SECRET, Duration.ofSeconds(-1)).verify(
            new JwtService(SECRET, Duration.ofSeconds(-1)).issue(1L, RoleType.PATIENT, "patient")));
  }

  @Test
  void requiresStrongSecret() {
    assertThrows(IllegalArgumentException.class, () -> new JwtService("short", Duration.ofMinutes(5)));
  }
}
