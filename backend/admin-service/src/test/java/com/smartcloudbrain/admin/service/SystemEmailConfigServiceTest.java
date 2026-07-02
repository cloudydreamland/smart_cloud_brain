package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

  @Test
  void getConfigReturnsDefaultWhenNoRowExists() {
    when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

    Map<String, Object> result = service.getConfig();

    assertEquals("", result.get("host"));
    assertEquals(465, result.get("port"));
    assertFalse((Boolean) result.get("enabled"));
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
  void sendTestDelegatesToNotificationClient() {
    Map<String, Object> response = Map.of("sent", true);
    when(notificationClient.sendEmail(eq("test@example.com"), anyString(), anyString()))
        .thenReturn(response);

    EmailConfigTestRequest request = new EmailConfigTestRequest("test@example.com");
    Map<String, Object> result = service.sendTest(request);

    assertEquals(true, result.get("sent"));
    verify(notificationClient).sendEmail(eq("test@example.com"), anyString(), anyString());
  }

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
