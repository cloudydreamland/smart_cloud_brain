package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.client.InternalNotificationClient;
import com.smartcloudbrain.admin.dto.admin.EmailConfigSaveRequest;
import com.smartcloudbrain.admin.dto.admin.EmailConfigTestRequest;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.common.security.TextCryptoService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemEmailConfigServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private TextCryptoService textCryptoService;
  @Mock private InternalNotificationClient notificationClient;
  @InjectMocks private SystemEmailConfigService service;

  // ── getConfig ──────────────────────────────────────────────

  @Test
  void getConfigReturnsDefaultWhenNoRowExists() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

    Map<String, Object> result = service.getConfig();

    assertEquals("", result.get("host"));
    assertEquals(465, result.get("port"));
    assertFalse((Boolean) result.get("enabled"));
    assertFalse((Boolean) result.get("passwordSet"));
    assertEquals("", result.get("username"));
    assertEquals("", result.get("fromAddress"));
    assertEquals("", result.get("fromName"));
    assertTrue((Boolean) result.get("sslEnabled"));
    assertFalse((Boolean) result.get("starttlsEnabled"));
  }

  @Test
  void getConfigReturnsExistingRow() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(existingRow()));

    Map<String, Object> result = service.getConfig();

    assertEquals("smtp.example.com", result.get("host"));
    assertEquals(587, result.get("port"));
    assertTrue((Boolean) result.get("passwordSet"));
    assertTrue((Boolean) result.get("enabled"));
  }

  @Test
  void getConfigHandlesNullValuesGracefully() {
    Map<String, Object> sparseRow = new LinkedHashMap<>();
    sparseRow.put("host", null);
    sparseRow.put("port", null);
    sparseRow.put("username", null);
    sparseRow.put("password_cipher", null);
    sparseRow.put("from_address", null);
    sparseRow.put("from_name", null);
    sparseRow.put("ssl_enabled", null);
    sparseRow.put("starttls_enabled", null);
    sparseRow.put("enabled", null);
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(sparseRow));

    Map<String, Object> result = service.getConfig();

    assertEquals("", result.get("host"));
    assertEquals(465, result.get("port"));  // fallback for non-parseable
    assertEquals("", result.get("username"));
    assertFalse((Boolean) result.get("passwordSet"));
    assertEquals("", result.get("fromAddress"));
    assertEquals("", result.get("fromName"));
    assertFalse((Boolean) result.get("sslEnabled"));
    assertFalse((Boolean) result.get("starttlsEnabled"));
    assertFalse((Boolean) result.get("enabled"));
  }

  @Test
  void getConfigHandlesNonNumberPortString() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("host", "smtp.test.com");
    row.put("port", "not_a_number");
    row.put("username", "user");
    row.put("password_cipher", "encrypted");
    row.put("from_address", "from@test.com");
    row.put("from_name", "Test");
    row.put("ssl_enabled", "true");
    row.put("starttls_enabled", "false");
    row.put("enabled", "yes");
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(row));

    Map<String, Object> result = service.getConfig();

    assertEquals(465, result.get("port"));    // fallback
    assertTrue((Boolean) result.get("sslEnabled"));  // Boolean.parseBoolean("true")
    assertFalse((Boolean) result.get("starttlsEnabled"));
    assertFalse((Boolean) result.get("enabled"));    // Boolean.parseBoolean("yes") is false
  }

  // ── save: update path ──────────────────────────────────────

  @Test
  void saveUpdatesExistingConfig() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(existingRow()));
    when(textCryptoService.encrypt("secret")).thenReturn("encrypted_secret");
    doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", 587, "user", "secret",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    Map<String, Object> result = service.save(request, user);

    verify(textCryptoService).encrypt("secret");
    verify(jdbcTemplate).update(anyString(), any(Object[].class));
    assertEquals("smtp.example.com", result.get("host"));
  }

  @Test
  void saveKeepsExistingCipherWhenPasswordIsBlank() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(existingRow()));
    doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", 587, "user", "   ",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    service.save(request, user);

    // Should NOT call encrypt since password is blank
    verify(textCryptoService, never()).encrypt(anyString());
  }

  @Test
  void saveKeepsExistingCipherWhenPasswordIsNull() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(existingRow()));
    doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", 587, "user", null,
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    service.save(request, user);

    verify(textCryptoService, never()).encrypt(anyString());
  }

  @Test
  void saveDefaultsPortTo465WhenNull() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(existingRow()));
    doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", null, "user", "secret",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    service.save(request, user);

    // Should succeed (port defaults to 465)
    verify(jdbcTemplate).update(anyString(), any(Object[].class));
  }

  // ── save: insert path ──────────────────────────────────────

  @Test
  void saveInsertsWhenNoExistingRow() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());
    when(textCryptoService.encrypt("newpass")).thenReturn("encrypted_newpass");
    // First update (UPDATE SET ...) returns 0, triggering INSERT path
    doReturn(0).doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.new.com", 465, "newuser", "newpass",
        "new@new.com", "NewName", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(2L, RoleType.ADMIN, "newadmin");

    service.save(request, user);

    // Should call update twice: once for UPDATE (returns 0), once for INSERT
    verify(jdbcTemplate, org.mockito.Mockito.times(2)).update(anyString(), any(Object[].class));
    verify(textCryptoService).encrypt("newpass");
  }

  // ── save: port validation ──────────────────────────────────

  @Test
  void saveThrowsWhenPortIsInvalid() {
    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", 0, "user", "secret",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    assertThrows(BusinessException.class, () -> service.save(request, user));
  }

  @Test
  void saveThrowsWhenPortExceedsMax() {
    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", 99999, "user", "secret",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    assertThrows(BusinessException.class, () -> service.save(request, user));
  }

  @Test
  void saveThrowsWhenPortIsNegative() {
    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", -1, "user", "secret",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    assertThrows(BusinessException.class, () -> service.save(request, user));
  }

  @Test
  void saveAcceptsPort65535() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(existingRow()));
    when(textCryptoService.encrypt("secret")).thenReturn("encrypted_secret");
    doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

    EmailConfigSaveRequest request = new EmailConfigSaveRequest(
        "smtp.test.com", 65535, "user", "secret",
        "from@test.com", "Test", true, false, true
    );
    AuthenticatedUser user = new AuthenticatedUser(1L, RoleType.ADMIN, "admin");

    service.save(request, user);

    verify(jdbcTemplate).update(anyString(), any(Object[].class));
  }

  // ── sendTest ───────────────────────────────────────────────

  @Test
  void sendTestDelegatesToNotificationClient() {
    Map<String, Object> response = Map.of("sent", true);
    when(notificationClient.sendEmail(eq("test@example.com"), anyString(), anyString()))
        .thenReturn(response);

    EmailConfigTestRequest request = new EmailConfigTestRequest("test@example.com");
    Map<String, Object> result = service.sendTest(request);

    assertEquals(true, result.get("sent"));
    verify(notificationClient).sendEmail(eq("test@example.com"), anyString(), anyString());
  }

  // ── helpers ────────────────────────────────────────────────

  private Map<String, Object> existingRow() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("host", "smtp.example.com");
    row.put("port", 587);
    row.put("username", "user@example.com");
    row.put("password_cipher", "encrypted");
    row.put("from_address", "no-reply@example.com");
    row.put("from_name", "Test");
    row.put("ssl_enabled", true);
    row.put("starttls_enabled", false);
    row.put("enabled", true);
    return row;
  }
}
