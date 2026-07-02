package com.smartcloudbrain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.entity.NotificationMessage;
import com.smartcloudbrain.notification.repository.NotificationMessageRepository;
import com.smartcloudbrain.notification.websocket.NotificationWebSocketHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceAdditionalTest {

  @Mock private NotificationMessageRepository notificationRepository;
  @Mock private CurrentUserService currentUserService;
  @Mock private NotificationWebSocketHandler webSocketHandler;

  private NotificationService service() {
    return new NotificationService(notificationRepository, currentUserService, webSocketHandler);
  }

  // ─────────────── list for different roles ───────────────

  @Test
  void list_patientRole_fetchesByPatientId() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(100L, RoleType.PATIENT, "patient"));
    when(notificationRepository.findByPatientId(100L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, null, null);
    assertEquals(1, result.size());
    verify(notificationRepository).findByPatientId(100L);
  }

  @Test
  void list_adminRole_fetchesAll() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(1L, RoleType.ADMIN, "admin"));
    when(notificationRepository.findAll()).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, null, null);
    assertEquals(1, result.size());
    verify(notificationRepository).findAll();
  }

  // ─────────────── list with sort options ───────────────

  @Test
  void list_sortByCreatedDesc() {
    NotificationMessage older = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now().minusDays(2));
    NotificationMessage newer = makeMessage(2L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(older, newer));

    var result = service().list(null, null, null, null, null, "CREATED_DESC");
    assertEquals(2L, result.get(0).get("notificationId"));
  }

  @Test
  void list_sortByCreatedAsc() {
    NotificationMessage older = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now().minusDays(2));
    NotificationMessage newer = makeMessage(2L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(older, newer));

    var result = service().list(null, null, null, null, null, "CREATED_ASC");
    assertEquals(1L, result.get(0).get("notificationId"));
  }

  @Test
  void list_sortByRiskDesc() {
    NotificationMessage low = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    NotificationMessage high = makeMessage(2L, 8L, "UNREAD", "PENDING", "HIGH", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(low, high));

    var result = service().list(null, null, null, null, null, "RISK_DESC");
    assertEquals(2L, result.get(0).get("notificationId"));
  }

  @Test
  void list_sortByUnreadFirst() {
    NotificationMessage read = makeMessage(1L, 8L, "READ", "HANDLED", "LOW", LocalDateTime.now());
    NotificationMessage unread = makeMessage(2L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(read, unread));

    var result = service().list(null, null, null, null, null, "UNREAD_FIRST");
    assertEquals(2L, result.get(0).get("notificationId"));
  }

  @Test
  void list_sortByHandledFirst() {
    NotificationMessage pending = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    NotificationMessage handled = makeMessage(2L, 8L, "READ", "HANDLED", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(pending, handled));

    var result = service().list(null, null, null, null, null, "HANDLED_FIRST");
    assertEquals(2L, result.get(0).get("notificationId"));
  }

  // ─────────────── list search filters ───────────────

  @Test
  void list_matchesSearchByType() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    msg.setType("PRESCRIPTION_RISK");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, "prescription", null);
    assertEquals(1, result.size());
  }

  @Test
  void list_matchesSearchByPatientId() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    msg.setPatientId(42L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, "42", null);
    assertEquals(1, result.size());
  }

  @Test
  void list_matchesSearchByPrescriptionId() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    msg.setPrescriptionId(99L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, "99", null);
    assertEquals(1, result.size());
  }

  @Test
  void list_matchesSearchByTriageRecordId() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    msg.setTriageRecordId(77L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, "77", null);
    assertEquals(1, result.size());
  }

  @Test
  void list_matchesSearchByMedicalRecordId() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    msg.setMedicalRecordId(55L);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, "55", null);
    assertEquals(1, result.size());
  }

  // ─────────────── handle edge cases ───────────────

  @Test
  void handle_invalidStatus_throws() {
    NotificationMessage msg = makeMessage(10L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(10L)).thenReturn(Optional.of(msg));

    assertThrows(BusinessException.class, () -> service().handle(10L, "INVALID"));
  }

  @Test
  void handle_ignoredStatus_setsIgnored() {
    NotificationMessage msg = makeMessage(10L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(10L)).thenReturn(Optional.of(msg));
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(inv -> inv.getArgument(0));

    var result = service().handle(10L, "IGNORED");
    assertEquals("IGNORED", result.get("handleStatus"));
    assertEquals("READ", result.get("readStatus"));
  }

  @Test
  void handle_notificationNotFound_throws() {
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service().handle(99L, "HANDLED"));
  }

  @Test
  void markRead_notificationNotFound_throws() {
    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service().markRead(99L));
  }

  // ─────────────── create edge cases ───────────────

  @Test
  void create_nullType_usesDefaultSystemNotice() {
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(inv -> {
      NotificationMessage m = inv.getArgument(0);
      m.setId(50L);
      return m;
    });

    var result = service().create(new NotificationCreateRequest(
        8L, 1L, null, null, null, null, null
    ));
    assertEquals("SYSTEM_NOTICE", result.get("type"));
    assertEquals("系统通知", result.get("title"));
  }

  @Test
  void create_emptyType_usesDefaultSystemNotice() {
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(inv -> {
      NotificationMessage m = inv.getArgument(0);
      m.setId(51L);
      return m;
    });

    var result = service().create(new NotificationCreateRequest(
        8L, 1L, null, "  ", null, null, null
    ));
    assertEquals("SYSTEM_NOTICE", result.get("type"));
  }

  @Test
  void create_nullContent_usesDefaultContent() {
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(inv -> {
      NotificationMessage m = inv.getArgument(0);
      m.setId(52L);
      return m;
    });

    var result = service().create(new NotificationCreateRequest(
        8L, 1L, null, "SYSTEM_NOTICE", "Test Title", null, null
    ));
    assertEquals("请查看通知详情。", result.get("content"));
  }

  @Test
  void create_triageAssignNoDuplicate_createsNew() {
    when(notificationRepository.findFirstByDoctorIdAndTriageRecordIdAndType(8L, 15L, "TRIAGE_ASSIGN"))
        .thenReturn(Optional.empty());
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(inv -> {
      NotificationMessage m = inv.getArgument(0);
      m.setId(60L);
      return m;
    });

    var result = service().create(new NotificationCreateRequest(
        8L, 1L, null, "TRIAGE_ASSIGN", "分诊分配", "患者分诊已分配", "", 15L, null
    ));
    assertEquals(60L, result.get("notificationId"));
    verify(webSocketHandler).sendToDoctor(eq(8L), any(String.class));
  }

  // ─────────────── notificationView null fields ───────────────

  @Test
  void notificationView_nullFields_usesDefaults() {
    NotificationMessage msg = new NotificationMessage();
    msg.setId(1L);
    msg.setDoctorId(8L);
    msg.setType("SYSTEM_NOTICE");
    msg.setTitle("Test");
    msg.setContent("Content");
    // All other fields null
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, null, null);
    assertEquals(1, result.size());
    var view = result.get(0);
    assertEquals(0L, view.get("patientId"));
    assertEquals(0L, view.get("prescriptionId"));
    assertEquals(0L, view.get("triageRecordId"));
    assertEquals(0L, view.get("medicalRecordId"));
    assertEquals("", view.get("riskLevel"));
    assertEquals("PENDING", view.get("handleStatus"));
    assertEquals("", view.get("createdAt"));
    assertEquals("", view.get("handledAt"));
  }

  // ─────────────── list with null createdAt ───────────────

  @Test
  void list_sortWithNullCreatedAt_handlesGracefully() {
    NotificationMessage withNull = makeMessage(1L, 8L, "UNREAD", "PENDING", "LOW", null);
    NotificationMessage withDate = makeMessage(2L, 8L, "UNREAD", "PENDING", "LOW", LocalDateTime.now());
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(withNull, withDate));

    var result = service().list(null, null, null, null, null, "CREATED_DESC");
    assertEquals(2, result.size());
  }

  @Test
  void list_defaultSort_ignoresAndHandlesGracefully() {
    NotificationMessage msg = makeMessage(1L, 8L, "UNREAD", "PENDING", "HIGH", LocalDateTime.now());
    msg.setHandleStatus(null);
    msg.setRiskLevel(null);
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(msg));

    var result = service().list(null, null, null, null, null, "UNKNOWN_SORT");
    assertEquals(1, result.size());
  }

  // ─────────────── helper ───────────────

  private static NotificationMessage makeMessage(Long id, Long doctorId, String readStatus,
      String handleStatus, String riskLevel, LocalDateTime createdAt) {
    NotificationMessage msg = new NotificationMessage();
    msg.setId(id);
    msg.setDoctorId(doctorId);
    msg.setType("PRESCRIPTION_RISK");
    msg.setTitle("Risk " + id);
    msg.setContent("Check dose");
    msg.setReadStatus(readStatus);
    msg.setHandleStatus(handleStatus);
    msg.setRiskLevel(riskLevel);
    msg.setCreatedAt(createdAt);
    return msg;
  }
}
