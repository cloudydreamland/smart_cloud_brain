package com.smartcloudbrain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
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
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationMessageRepository notificationRepository;
  @Mock private CurrentUserService currentUserService;
  @Mock private NotificationWebSocketHandler webSocketHandler;

  @Test
  void createPersistsUnreadNotificationAndPushesDoctorMessage() {
    NotificationService service = new NotificationService(notificationRepository, null, webSocketHandler);
    when(notificationRepository.save(org.mockito.ArgumentMatchers.any(NotificationMessage.class)))
        .thenAnswer(invocation -> {
          NotificationMessage message = invocation.getArgument(0);
          message.setId(99L);
          return message;
        });

    var result = service.create(new NotificationCreateRequest(
        8L,
        1L,
        15L,
        "PRESCRIPTION_RISK",
        "Risk \"notice\"",
        "Check dose",
        "HIGH"
    ));

    assertEquals("UNREAD", result.get("readStatus"));
    assertEquals("PENDING", result.get("handleStatus"));
    assertEquals(99L, result.get("notificationId"));
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(webSocketHandler).sendToDoctor(eq(8L), payloadCaptor.capture());
    String payload = payloadCaptor.getValue();
    assertTrue(payload.contains("\"notificationId\":99"));
    assertTrue(payload.contains("Risk \\\"notice\\\""));
    assertTrue(payload.contains("\"riskLevel\":\"HIGH\""));
  }

  @Test
  void markReadOnlyUpdatesCurrentDoctorNotification() {
    NotificationService service = new NotificationService(notificationRepository, currentUserService, webSocketHandler);
    NotificationMessage message = new NotificationMessage();
    message.setId(10L);
    message.setDoctorId(8L);
    message.setReadStatus("UNREAD");
    message.setType("PRESCRIPTION_RISK");
    message.setTitle("Risk");
    message.setContent("Check dose");

    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(10L)).thenReturn(Optional.of(message));
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.markRead(10L);

    assertEquals("READ", result.get("readStatus"));
    verify(notificationRepository).findById(10L);
  }

  @Test
  void handleOnlyUpdatesCurrentDoctorNotification() {
    NotificationService service = new NotificationService(notificationRepository, currentUserService, webSocketHandler);
    NotificationMessage message = new NotificationMessage();
    message.setId(10L);
    message.setDoctorId(8L);
    message.setReadStatus("UNREAD");
    message.setHandleStatus("PENDING");
    message.setType("PRESCRIPTION_RISK");
    message.setTitle("Risk");
    message.setContent("Check dose");

    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(10L)).thenReturn(Optional.of(message));
    when(notificationRepository.save(any(NotificationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.handle(10L, "HANDLED");

    assertEquals("READ", result.get("readStatus"));
    assertEquals("HANDLED", result.get("handleStatus"));
    assertTrue(String.valueOf(result.get("handledAt")).length() > 0);
  }

  @Test
  void markReadRejectsAnotherDoctorsNotification() {
    NotificationService service = new NotificationService(notificationRepository, currentUserService, webSocketHandler);
    NotificationMessage message = new NotificationMessage();
    message.setId(10L);
    message.setDoctorId(9L);

    when(currentUserService.require(RoleType.DOCTOR)).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findById(10L)).thenReturn(Optional.of(message));

    assertThrows(BusinessException.class, () -> service.markRead(10L));
    verify(notificationRepository, never()).save(any());
  }

  @Test
  void listsAllAndFilteredNotificationsForCurrentDoctor() {
    NotificationMessage message = new NotificationMessage();
    message.setId(10L);
    message.setDoctorId(8L);
    message.setType("PRESCRIPTION_RISK");
    message.setTitle("Risk");
    message.setContent("Check dose");
    message.setReadStatus("UNREAD");
    message.setHandleStatus("PENDING");
    message.setRiskLevel("HIGH");
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(message));
    NotificationService service = new NotificationService(notificationRepository, currentUserService, webSocketHandler);

    assertEquals(1, service.list(null, null, null, null, null, null).size());
    assertEquals(1, service.list("UNREAD", "PENDING", "PRESCRIPTION_RISK", "HIGH", "dose", null).size());
    assertEquals(0, service.list("READ", null, null, null, null, null).size());
  }

  @Test
  void listsPendingHighRiskBeforeHandledRecentNotificationsByDefault() {
    NotificationMessage handledRecent = notification(1L, "READ", "HANDLED", "LOW", LocalDateTime.now());
    NotificationMessage pendingOlder = notification(2L, "UNREAD", "PENDING", "HIGH", LocalDateTime.now().minusDays(1));
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(8L, RoleType.DOCTOR, "doctor"));
    when(notificationRepository.findByDoctorId(8L)).thenReturn(List.of(handledRecent, pendingOlder));
    NotificationService service = new NotificationService(notificationRepository, currentUserService, webSocketHandler);

    var rows = service.list(null, null, null, null, null, null);

    assertEquals(2L, rows.get(0).get("notificationId"));
  }

  @Test
  void createDeduplicatesTriageAssignNotifications() {
    NotificationMessage existing = notification(88L, "UNREAD", "PENDING", "", LocalDateTime.now());
    existing.setType("TRIAGE_ASSIGN");
    existing.setTriageRecordId(15L);
    when(notificationRepository.findFirstByDoctorIdAndTriageRecordIdAndType(8L, 15L, "TRIAGE_ASSIGN"))
        .thenReturn(Optional.of(existing));
    NotificationService service = new NotificationService(notificationRepository, null, webSocketHandler);

    var result = service.create(new NotificationCreateRequest(
        8L, 1L, null, "TRIAGE_ASSIGN", "分诊分配", "患者分诊已分配", "", 15L, null
    ));

    assertEquals(88L, result.get("notificationId"));
    verify(webSocketHandler, never()).sendToDoctor(any(), any());
  }

  private static NotificationMessage notification(Long id, String readStatus, String handleStatus, String riskLevel, LocalDateTime createdAt) {
    NotificationMessage message = new NotificationMessage();
    message.setId(id);
    message.setDoctorId(8L);
    message.setType("PRESCRIPTION_RISK");
    message.setTitle("Risk " + id);
    message.setContent("Check dose");
    message.setReadStatus(readStatus);
    message.setHandleStatus(handleStatus);
    message.setRiskLevel(riskLevel);
    message.setCreatedAt(createdAt);
    return message;
  }
}
