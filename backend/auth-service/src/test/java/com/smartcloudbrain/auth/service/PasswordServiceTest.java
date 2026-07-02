package com.smartcloudbrain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.PasswordHashService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

  @Mock private PasswordHashService passwordHashService;
  @InjectMocks private PasswordService passwordService;

  @Test
  void encodeDelegatesToPasswordHashService() {
    when(passwordHashService.encode("raw")).thenReturn("{bcrypt}encoded");

    String result = passwordService.encode("raw");

    assertEquals("{bcrypt}encoded", result);
    verify(passwordHashService).encode("raw");
  }

  @Test
  void assertMatchesPassesWhenPasswordsMatch() {
    when(passwordHashService.matches("raw", "stored")).thenReturn(true);

    passwordService.assertMatches("raw", "stored");

    verify(passwordHashService).matches("raw", "stored");
  }

  @Test
  void assertMatchesThrowsWhenPasswordsDoNotMatch() {
    when(passwordHashService.matches("wrong", "stored")).thenReturn(false);

    assertThrows(BusinessException.class,
        () -> passwordService.assertMatches("wrong", "stored"));
  }

  @Test
  void requiresUpgradeDelegatesToPasswordHashService() {
    when(passwordHashService.requiresUpgrade("{plain}123")).thenReturn(true);
    when(passwordHashService.requiresUpgrade("{bcrypt}hash")).thenReturn(false);

    assertTrue(passwordService.requiresUpgrade("{plain}123"));
    assertFalse(passwordService.requiresUpgrade("{bcrypt}hash"));
  }
}
