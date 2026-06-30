package com.smartcloudbrain.notification.service;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.TextCryptoService;
import com.smartcloudbrain.notification.dto.EmailSendRequest;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

  private final JdbcTemplate jdbcTemplate;
  private final TextCryptoService textCryptoService;

  public EmailNotificationService(JdbcTemplate jdbcTemplate, TextCryptoService textCryptoService) {
    this.jdbcTemplate = jdbcTemplate;
    this.textCryptoService = textCryptoService;
  }

  public Map<String, Object> send(EmailSendRequest request) {
    Map<String, Object> config = currentConfig();
    if (!bool(config.get("enabled"))) {
      throw new BusinessException(400, "SMTP email is disabled");
    }
    String host = text(config.get("host"));
    String username = text(config.get("username"));
    String fromAddress = text(config.get("from_address"));
    if (host.isBlank() || fromAddress.isBlank()) {
      throw new BusinessException(400, "SMTP config is incomplete");
    }
    try {
      JavaMailSenderImpl sender = new JavaMailSenderImpl();
      sender.setHost(host);
      sender.setPort(number(config.get("port"), 465));
      sender.setUsername(username);
      String passwordCipher = text(config.get("password_cipher"));
      if (!passwordCipher.isBlank()) {
        sender.setPassword(textCryptoService.decrypt(passwordCipher));
      }
      Properties props = sender.getJavaMailProperties();
      props.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
      props.put("mail.smtp.ssl.enable", String.valueOf(bool(config.get("ssl_enabled"))));
      props.put("mail.smtp.starttls.enable", String.valueOf(bool(config.get("starttls_enabled"))));
      props.put("mail.smtp.connectiontimeout", "8000");
      props.put("mail.smtp.timeout", "8000");
      props.put("mail.smtp.writetimeout", "8000");

      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      String fromName = text(config.get("from_name"));
      if (fromName.isBlank()) {
        helper.setFrom(fromAddress);
      } else {
        helper.setFrom(fromAddress, fromName);
      }
      helper.setTo(request.toAddress().trim());
      helper.setSubject(request.subject().trim());
      helper.setText(request.content(), false);
      sender.send(message);
      return Map.of("sent", true, "toAddress", request.toAddress().trim());
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(500, "email send failed: " + ex.getMessage());
    }
  }

  private Map<String, Object> currentConfig() {
    return jdbcTemplate.queryForList("SELECT * FROM system_email_config WHERE config_key = 'SMTP'").stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(400, "SMTP config is not initialized"));
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
