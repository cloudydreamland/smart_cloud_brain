package com.smartcloudbrain.triage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.triage.entity.TriageRecord;
import com.smartcloudbrain.triage.repository.PatientRepository;
import com.smartcloudbrain.triage.repository.TriageRecordRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {

  @Mock private AiGatewayService aiGatewayService;
  @Mock private TriageRecordRepository triageRecordRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private CurrentUserService currentUserService;
  @Mock private RedisRateLimiter redisRateLimiter;
  @InjectMocks private TriageService triageService;

  @BeforeEach
  void setUp() {
    lenient().when(redisRateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
  }

  @Test
  void consultStoresAiRecommendedRecord() {
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    when(aiGatewayService.triage(any())).thenReturn(new TriageResponse(
        "Cardiology", "CARD", "senior", "LOW", 0.91, List.of(2L, 3L), "stable", false));
    when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(invocation -> {
      TriageRecord record = invocation.getArgument(0);
      record.setId(10L);
      return record;
    });

    Map<String, Object> result = triageService.consult(new TriageRequest(1L, "chest pain", "pain", null, null, null, null));

    assertEquals(10L, result.get("triageRecordId"));
    assertEquals("Cardiology", result.get("recommendedDepartment"));
    assertEquals("AI_RECOMMENDED", result.get("status"));
    assertEquals(List.of(2L, 3L), result.get("recommendedDoctorIds"));
  }

  @Test
  void consultMarksDegradedAiResponseForManualHandling() {
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(patientRepository.findById(1L)).thenReturn(Optional.empty());
    when(aiGatewayService.triage(any())).thenReturn(new TriageResponse("General", "GEN", List.of(), "fallback", true));
    when(triageRecordRepository.save(any(TriageRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> result = triageService.consult(new TriageRequest(null, "fever", "fever", 30, "MALE", "", ""));

    assertEquals("MANUAL_REQUIRED", result.get("status"));
    assertEquals(true, result.get("degraded"));
  }

  @Test
  void consultRejectsOtherPatientId() {
    when(currentUserService.require(RoleType.PATIENT)).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));

    assertThrows(BusinessException.class,
        () -> triageService.consult(new TriageRequest(2L, "fever", "fever", null, null, null, null)));
  }

  @Test
  void listUsesRoleScope() {
    TriageRecord own = record(1L, 1L);
    TriageRecord other = record(2L, 2L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.PATIENT, "Alice"));
    when(triageRecordRepository.findByPatientId(1L)).thenReturn(List.of(own));

    assertEquals(1, triageService.list().size());

    when(currentUserService.get()).thenReturn(new AuthenticatedUser(9L, RoleType.ADMIN, "admin"));
    when(triageRecordRepository.findAll()).thenReturn(List.of(own, other));
    assertEquals(2, triageService.list().size());

    TriageRecord assigned = record(3L, 3L);
    assigned.setAssignedDoctorId(8L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(triageRecordRepository.findByAssignedDoctorId(8L)).thenReturn(List.of(assigned));
    assertEquals(1, triageService.list().size());
  }

  private static TriageRecord record(Long id, Long patientId) {
    TriageRecord record = new TriageRecord();
    record.setId(id);
    record.setPatientId(patientId);
    record.setChiefComplaint("fever");
    record.setStatus("AI_RECOMMENDED");
    return record;
  }
}
