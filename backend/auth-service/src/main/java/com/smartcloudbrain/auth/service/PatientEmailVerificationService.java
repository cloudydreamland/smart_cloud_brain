package com.smartcloudbrain.auth.service;

import com.smartcloudbrain.auth.client.InternalNotificationClient;
import com.smartcloudbrain.auth.dto.patient.EmailCodeSendRequest;
import com.smartcloudbrain.auth.entity.PatientEmailVerificationCode;
import com.smartcloudbrain.auth.repository.PatientEmailVerificationCodeRepository;
import com.smartcloudbrain.auth.repository.PatientRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientEmailVerificationService {

  private static final String PURPOSE_REGISTER = "REGISTER";
  private static final int MAX_FAILED_ATTEMPTS = 5;

  private final PatientEmailVerificationCodeRepository codeRepository;
  private final PatientRepository patientRepository;
  private final RedisRateLimiter redisRateLimiter;
  private final InternalNotificationClient notificationClient;
  private final SecureRandom secureRandom = new SecureRandom();
  private final String hashSecret;
  private final int ttlMinutes;

  public PatientEmailVerificationService(
      PatientEmailVerificationCodeRepository codeRepository,
      PatientRepository patientRepository,
      RedisRateLimiter redisRateLimiter,
      InternalNotificationClient notificationClient,
      @Value("${patient.email-code.hash-secret:${JWT_SECRET:smart-cloud-brain-local-secret-please-change}}") String hashSecret,
      @Value("${patient.email-code.ttl-minutes:10}") int ttlMinutes
  ) {
    this.codeRepository = codeRepository;
    this.patientRepository = patientRepository;
    this.redisRateLimiter = redisRateLimiter;
    this.notificationClient = notificationClient;
    this.hashSecret = hashSecret;
    this.ttlMinutes = ttlMinutes <= 0 ? 10 : ttlMinutes;
  }

  @Transactional
  public Map<String, Object> sendRegisterCode(EmailCodeSendRequest request, String clientIp) {
    String email = normalizeEmail(request.email());
    String phone = normalize(request.phone());
    if (email.isBlank()) {
      throw new BusinessException(400, "email is required");
    }
    patientRepository.findByEmail(email).ifPresent(patient -> {
      throw new BusinessException(409, "email already registered");
    });
    if (!phone.isBlank() && patientRepository.findByPhone(phone).isPresent()) {
      throw new BusinessException(409, "phone already registered");
    }
    LocalDateTime now = LocalDateTime.now();
    codeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, PURPOSE_REGISTER).ifPresent(previous -> {
      if (previous.getCreatedAt() != null && previous.getCreatedAt().isAfter(now.minusSeconds(60))) {
        throw new BusinessException(429, "email code sent too frequently");
      }
    });
    if (!redisRateLimiter.allow("rate:email-code:email:" + email, 5, Duration.ofHours(1))) {
      throw new BusinessException(429, "email code sent too frequently");
    }
    String ip = normalize(clientIp);
    if (!ip.isBlank() && !redisRateLimiter.allow("rate:email-code:ip:" + ip, 20, Duration.ofHours(1))) {
      throw new BusinessException(429, "email code sent too frequently");
    }

    String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    PatientEmailVerificationCode entity = new PatientEmailVerificationCode();
    entity.setEmail(email);
    entity.setPhone(phone.isBlank() ? null : phone);
    entity.setPurpose(PURPOSE_REGISTER);
    entity.setCodeHash(hashCode(email, PURPOSE_REGISTER, code));
    entity.setExpiresAt(now.plusMinutes(ttlMinutes));
    entity.setFailedAttempts(0);
    entity.setLastSentIp(ip);
    entity.setUpdatedAt(now);
    codeRepository.save(entity);

    notificationClient.sendEmail(
        email,
        "智慧云脑患者注册验证码",
        "您的患者注册邮箱验证码为：" + code + "，" + ttlMinutes + "分钟内有效。请勿向他人泄露。"
    );
    return Map.of("sent", true, "expiresInSeconds", ttlMinutes * 60);
  }

  @Transactional
  public void verifyRegisterCode(String emailValue, String codeValue) {
    String email = normalizeEmail(emailValue);
    String code = normalize(codeValue);
    if (email.isBlank() || code.isBlank()) {
      throw new BusinessException(400, "email code is required");
    }
    LocalDateTime now = LocalDateTime.now();
    PatientEmailVerificationCode codeRecord = codeRepository
        .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, PURPOSE_REGISTER)
        .orElseThrow(() -> new BusinessException(400, "email code is invalid"));
    if (codeRecord.getConsumedAt() != null) {
      throw new BusinessException(400, "email code already consumed");
    }
    if (codeRecord.getExpiresAt() == null || codeRecord.getExpiresAt().isBefore(now)) {
      throw new BusinessException(400, "email code expired");
    }
    int failedAttempts = codeRecord.getFailedAttempts() == null ? 0 : codeRecord.getFailedAttempts();
    if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
      throw new BusinessException(400, "email code attempts exceeded");
    }
    if (!MessageDigest.isEqual(
        hashCode(email, PURPOSE_REGISTER, code).getBytes(StandardCharsets.UTF_8),
        codeRecord.getCodeHash().getBytes(StandardCharsets.UTF_8)
    )) {
      codeRecord.setFailedAttempts(failedAttempts + 1);
      codeRecord.setUpdatedAt(now);
      codeRepository.save(codeRecord);
      throw new BusinessException(400, "email code is invalid");
    }
    codeRecord.setConsumedAt(now);
    codeRecord.setUpdatedAt(now);
    codeRepository.save(codeRecord);
  }

  String hashCode(String email, String purpose, String code) {
    try {
      String payload = normalizeEmail(email) + ":" + normalize(purpose).toUpperCase(Locale.ROOT) + ":" + normalize(code) + ":" + hashSecret;
      return "{sha256}" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash email code", ex);
    }
  }

  private String normalizeEmail(String value) {
    return normalize(value).toLowerCase(Locale.ROOT);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }
}
