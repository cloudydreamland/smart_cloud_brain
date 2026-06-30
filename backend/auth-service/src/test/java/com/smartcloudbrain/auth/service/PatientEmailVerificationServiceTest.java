package com.smartcloudbrain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.auth.client.InternalNotificationClient;
import com.smartcloudbrain.auth.dto.patient.EmailCodeSendRequest;
import com.smartcloudbrain.auth.entity.PatientEmailVerificationCode;
import com.smartcloudbrain.auth.repository.PatientEmailVerificationCodeRepository;
import com.smartcloudbrain.auth.repository.PatientRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientEmailVerificationServiceTest {

  @Mock private PatientEmailVerificationCodeRepository codeRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private RedisRateLimiter redisRateLimiter;
  @Mock private InternalNotificationClient notificationClient;

  private PatientEmailVerificationService service;

  @BeforeEach
  void setUp() {
    service = new PatientEmailVerificationService(
        codeRepository,
        patientRepository,
        redisRateLimiter,
        notificationClient,
        "test-secret",
        10
    );
    lenient().when(redisRateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
  }

  @Test
  void sendsRegisterEmailCode() {
    when(patientRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
    when(patientRepository.findByPhone("13800000001")).thenReturn(Optional.empty());
    when(codeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("alice@example.com", "REGISTER"))
        .thenReturn(Optional.empty());
    when(notificationClient.sendEmail(anyString(), anyString(), anyString())).thenReturn(Map.of("sent", true));

    Map<String, Object> result = service.sendRegisterCode(
        new EmailCodeSendRequest("Alice@Example.com", "13800000001", "REGISTER"),
        "127.0.0.1"
    );

    ArgumentCaptor<PatientEmailVerificationCode> captor = ArgumentCaptor.forClass(PatientEmailVerificationCode.class);
    verify(codeRepository).save(captor.capture());
    assertEquals(true, result.get("sent"));
    assertEquals("alice@example.com", captor.getValue().getEmail());
    assertEquals("REGISTER", captor.getValue().getPurpose());
    assertNotNull(captor.getValue().getCodeHash());
    verify(notificationClient).sendEmail(anyString(), anyString(), anyString());
  }

  @Test
  void rejectsWrongEmailCode() {
    PatientEmailVerificationCode record = codeRecord("111111", LocalDateTime.now().plusMinutes(5), null);
    when(codeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("alice@example.com", "REGISTER"))
        .thenReturn(Optional.of(record));

    assertThrows(BusinessException.class, () -> service.verifyRegisterCode("alice@example.com", "222222"));

    assertEquals(1, record.getFailedAttempts());
    verify(codeRepository).save(record);
  }

  @Test
  void rejectsExpiredEmailCode() {
    PatientEmailVerificationCode record = codeRecord("111111", LocalDateTime.now().minusSeconds(1), null);
    when(codeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("alice@example.com", "REGISTER"))
        .thenReturn(Optional.of(record));

    assertThrows(BusinessException.class, () -> service.verifyRegisterCode("alice@example.com", "111111"));
  }

  @Test
  void rejectsConsumedEmailCode() {
    PatientEmailVerificationCode record = codeRecord("111111", LocalDateTime.now().plusMinutes(5), LocalDateTime.now());
    when(codeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("alice@example.com", "REGISTER"))
        .thenReturn(Optional.of(record));

    assertThrows(BusinessException.class, () -> service.verifyRegisterCode("alice@example.com", "111111"));
  }

  @Test
  void consumesEmailCodeOnSuccess() {
    PatientEmailVerificationCode record = codeRecord("111111", LocalDateTime.now().plusMinutes(5), null);
    when(codeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("alice@example.com", "REGISTER"))
        .thenReturn(Optional.of(record));

    service.verifyRegisterCode("alice@example.com", "111111");

    assertNotNull(record.getConsumedAt());
    verify(codeRepository).save(record);
  }

  private PatientEmailVerificationCode codeRecord(String code, LocalDateTime expiresAt, LocalDateTime consumedAt) {
    PatientEmailVerificationCode record = new PatientEmailVerificationCode();
    record.setEmail("alice@example.com");
    record.setPurpose("REGISTER");
    record.setCodeHash(service.hashCode("alice@example.com", "REGISTER", code));
    record.setExpiresAt(expiresAt);
    record.setConsumedAt(consumedAt);
    record.setFailedAttempts(0);
    return record;
  }
}

