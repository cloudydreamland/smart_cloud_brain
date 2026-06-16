package com.smartcloudbrain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.entity.NotificationMessage;
import com.smartcloudbrain.notification.repository.NotificationMessageRepository;
import com.smartcloudbrain.notification.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationMessageRepository notificationRepository;
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
}
