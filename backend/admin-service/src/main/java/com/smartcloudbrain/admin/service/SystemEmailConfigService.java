package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.admin.client.InternalNotificationClient;
import com.smartcloudbrain.admin.dto.admin.EmailConfigSaveRequest;
import com.smartcloudbrain.admin.dto.admin.EmailConfigTestRequest;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.TextCryptoService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemEmailConfigService {

  private final JdbcTemplate jdbcTemplate;
  private final TextCryptoService textCryptoService;
  private final InternalNotificationClient notificationClient;

  public SystemEmailConfigService(
      JdbcTemplate jdbcTemplate,
      TextCryptoService textCryptoService,
      InternalNotificationClient notificationClient
  ) {
    this.jdbcTemplate = jdbcTemplate;
    this.textCryptoService = textCryptoService;
    this.notificationClient = notificationClient;
  }

  public Map<String, Object> getConfig() {
    return view(currentOrDefault());
  }

  @Transactional
  public Map<String, Object> save(EmailConfigSaveRequest request, AuthenticatedUser user) {
    int port = request.port() == null ? 465 : request.port();
    if (port <= 0 || port > 65535) {
      throw new BusinessException(400, "SMTP port is invalid");
    }
    Map<String, Object> current = currentOrDefault();
    String existingCipher = text(current.get("password_cipher"));
    String passwordCipher = request.password() == null || request.password().isBlank()
        ? existingCipher
        : textCryptoService.encrypt(request.password().trim());
    int changed = jdbcTemplate.update("""
        UPDATE system_email_config
        SET host = ?, port = ?, username = ?, password_cipher = ?, from_address = ?, from_name = ?,
            ssl_enabled = ?, starttls_enabled = ?, enabled = ?, updated_by = ?, updated_at = ?
        WHERE config_key = 'SMTP'
        """,
        clean(request.host()),
        port,
        clean(request.username()),
        blankToNull(passwordCipher),
        clean(request.fromAddress()).toLowerCase(),
        clean(request.fromName()),
        bool(request.sslEnabled()),
        bool(request.starttlsEnabled()),
        bool(request.enabled()),
        user.userId(),
        LocalDateTime.now()
    );
    if (changed == 0) {
      jdbcTemplate.update("""
          INSERT INTO system_email_config (
            config_key, host, port, username, password_cipher, from_address, from_name,
            ssl_enabled, starttls_enabled, enabled, created_by, updated_by
          ) VALUES ('SMTP', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          clean(request.host()),
          port,
          clean(request.username()),
          blankToNull(passwordCipher),
          clean(request.fromAddress()).toLowerCase(),
          clean(request.fromName()),
          bool(request.sslEnabled()),
          bool(request.starttlsEnabled()),
          bool(request.enabled()),
          user.userId(),
          user.userId()
      );
    }
    return getConfig();
  }

  public Map<String, Object> sendTest(EmailConfigTestRequest request) {
    return notificationClient.sendEmail(
        request.toAddress().trim(),
        "智慧云脑 SMTP 测试邮件",
        "这是一封来自智慧云脑诊疗平台的 SMTP 配置测试邮件。"
    );
  }

  private Map<String, Object> currentOrDefault() {
    return jdbcTemplate.queryForList("SELECT * FROM system_email_config WHERE config_key = 'SMTP'").stream()
        .findFirst()
        .orElseGet(() -> {
          Map<String, Object> defaults = new LinkedHashMap<>();
          defaults.put("host", "");
          defaults.put("port", 465);
          defaults.put("username", "");
          defaults.put("password_cipher", "");
          defaults.put("from_address", "");
          defaults.put("from_name", "");
          defaults.put("ssl_enabled", true);
          defaults.put("starttls_enabled", false);
          defaults.put("enabled", false);
          return defaults;
        });
  }

  private Map<String, Object> view(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("host", text(row.get("host")));
    view.put("port", number(row.get("port"), 465));
    view.put("username", text(row.get("username")));
    view.put("passwordSet", !text(row.get("password_cipher")).isBlank());
    view.put("fromAddress", text(row.get("from_address")));
    view.put("fromName", text(row.get("from_name")));
    view.put("sslEnabled", bool(row.get("ssl_enabled")));
    view.put("starttlsEnabled", bool(row.get("starttls_enabled")));
    view.put("enabled", bool(row.get("enabled")));
    return view;
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private int number(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(text(value));
    } catch (Exception ex) {
      return fallback;
    }
  }

  private boolean bool(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    return Boolean.parseBoolean(text(value));
  }
}
