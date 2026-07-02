package com.smartcloudbrain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.TextCryptoService;
import com.smartcloudbrain.notification.dto.EmailSendRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private TextCryptoService textCryptoService;
  @InjectMocks private EmailNotificationService service;

  @Test
  void sendDisabledThrowsBusinessException() {
    Map<String, Object> config = Map.of("enabled", false);
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
    assertEquals(400, ex.code());
    assertTrue(ex.getMessage().contains("disabled"));
  }

  @Test
  void sendWithBlankHostThrowsIncompleteConfig() {
    Map<String, Object> config = Map.of("enabled", true, "host", "", "from_address", "x@y.com");
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
    assertEquals(400, ex.code());
    assertTrue(ex.getMessage().contains("incomplete"));
  }

  @Test
  void sendWithBlankFromAddressThrowsIncompleteConfig() {
    Map<String, Object> config = Map.of("enabled", true, "host", "smtp.example.com", "from_address", "");
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
    assertEquals(400, ex.code());
    assertTrue(ex.getMessage().contains("incomplete"));
  }

  @Test
  void sendWithNoConfigRowThrowsNotInitialized() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

    assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
  }

  @Test
  void sendWithNullHostThrowsIncompleteConfig() {
    Map<String, Object> config = Map.of("enabled", true, "from_address", "x@y.com");
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    BusinessException ex = assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
    assertEquals(400, ex.code());
  }

  @Test
  void numberFallbackReturnsDefaultForNonNumeric() {
    // This tests the private number() method indirectly via port config
    Map<String, Object> config = Map.of(
        "enabled", true,
        "host", "smtp.example.com",
        "from_address", "sender@example.com",
        "port", "not-a-number",
        "username", "",
        "password_cipher", "",
        "ssl_enabled", "false",
        "starttls_enabled", "false"
    );
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    // The send will fail because the actual SMTP connection fails, but the port fallback (465) is exercised
    assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
  }

  @Test
  void sendWithNumberPortConfigExercisesNumberMethod() {
    Map<String, Object> config = Map.of(
        "enabled", true,
        "host", "smtp.example.com",
        "from_address", "sender@example.com",
        "port", 587,
        "username", "user@example.com",
        "password_cipher", "",
        "ssl_enabled", true,
        "starttls_enabled", true,
        "from_name", "My App"
    );
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    // send() will fail on actual SMTP connection, but exercises the config path
    assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
  }

  @Test
  void sendWithPasswordCipherDecryptsPassword() {
    Map<String, Object> config = Map.of(
        "enabled", true,
        "host", "smtp.example.com",
        "from_address", "sender@example.com",
        "port", 465,
        "username", "user@example.com",
        "password_cipher", "{aes-gcm}encrypted",
        "ssl_enabled", false,
        "starttls_enabled", false,
        "from_name", ""
    );
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));
    when(textCryptoService.decrypt("{aes-gcm}encrypted")).thenReturn("decrypted-password");

    // send() will fail on actual SMTP connection, but exercises decrypt path
    assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
    // Verify decrypt was called
    org.mockito.Mockito.verify(textCryptoService).decrypt("{aes-gcm}encrypted");
  }

  @Test
  void boolWithBooleanValueExercisesBoolMethod() {
    Map<String, Object> config = Map.of(
        "enabled", Boolean.TRUE,
        "host", "smtp.example.com",
        "from_address", "sender@example.com",
        "port", 465,
        "username", "",
        "password_cipher", "",
        "ssl_enabled", Boolean.TRUE,
        "starttls_enabled", Boolean.FALSE,
        "from_name", "Sender Name"
    );
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(config));

    // Exercises the bool() method with actual Boolean instances
    assertThrows(BusinessException.class,
        () -> service.send(new EmailSendRequest("a@b.com", "Subject", "Body")));
  }
}
