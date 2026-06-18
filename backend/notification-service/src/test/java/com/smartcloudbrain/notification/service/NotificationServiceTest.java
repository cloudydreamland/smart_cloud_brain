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
import java.util.Optional;
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
}
