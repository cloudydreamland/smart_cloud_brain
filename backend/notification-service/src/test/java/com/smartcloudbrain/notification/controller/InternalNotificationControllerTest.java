package com.smartcloudbrain.notification.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.notification.dto.EmailSendRequest;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.service.EmailNotificationService;
import com.smartcloudbrain.notification.service.NotificationService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalNotificationControllerTest {

  @Mock private NotificationService notificationService;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private InternalRequestGuard internalRequestGuard;
  @InjectMocks private InternalNotificationController controller;

  @Test
  void createDelegatesToNotificationService() {
    NotificationCreateRequest request = new NotificationCreateRequest(
        8L, 1L, 15L, "PRESCRIPTION_RISK", "Title", "Content", "HIGH"
    );
    Map<String, Object> expected = Map.of("notificationId", 99L);
    when(notificationService.create(request)).thenReturn(expected);

    var result = controller.create(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(internalRequestGuard).requireServiceRequest();
    verify(notificationService).create(request);
  }

  @Test
  void sendEmailDelegatesToEmailService() {
    EmailSendRequest request = new EmailSendRequest("a@b.com", "Subject", "Body");
    Map<String, Object> expected = Map.of("sent", true, "toAddress", "a@b.com");
    when(emailNotificationService.send(request)).thenReturn(expected);

    var result = controller.sendEmail(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(internalRequestGuard).requireServiceRequest();
    verify(emailNotificationService).send(request);
  }
}
