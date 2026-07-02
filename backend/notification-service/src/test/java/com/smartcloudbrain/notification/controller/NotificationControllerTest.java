package com.smartcloudbrain.notification.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

  @Mock private NotificationService notificationService;
  @InjectMocks private NotificationController controller;

  @Test
  void listReturnsSuccessWithData() {
    List<Map<String, Object>> expected = List.of(Map.of("id", 1L, "title", "Test"));
    when(notificationService.list(null, null, null, null, null, null)).thenReturn(expected);

    var result = controller.list(null, null, null, null, null, null);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void listWithAllFiltersDelegatesToService() {
    List<Map<String, Object>> expected = List.of(Map.of("id", 2L));
    when(notificationService.list("UNREAD", "PENDING", "PRESCRIPTION_RISK", "HIGH", "dose", "CREATED_DESC"))
        .thenReturn(expected);

    var result = controller.list("UNREAD", "PENDING", "PRESCRIPTION_RISK", "HIGH", "dose", "CREATED_DESC");

    assertEquals(expected, result.data());
    verify(notificationService).list("UNREAD", "PENDING", "PRESCRIPTION_RISK", "HIGH", "dose", "CREATED_DESC");
  }

  @Test
  void readReturnsSuccessWithResult() {
    Map<String, Object> expected = Map.of("readStatus", "READ");
    when(notificationService.markRead(10L)).thenReturn(expected);

    var result = controller.read(Map.of("notificationId", 10L));

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(notificationService).markRead(10L);
  }

  @Test
  void handleReturnsSuccessWithResult() {
    Map<String, Object> expected = Map.of("handleStatus", "HANDLED");
    when(notificationService.handle(10L, "HANDLED")).thenReturn(expected);

    var result = controller.handle(Map.of("notificationId", 10L, "handleStatus", "HANDLED"));

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(notificationService).handle(10L, "HANDLED");
  }

  @Test
  void handleWithStringNotificationIdParsesCorrectly() {
    Map<String, Object> expected = Map.of("handleStatus", "IGNORED");
    when(notificationService.handle(5L, "IGNORED")).thenReturn(expected);

    var result = controller.handle(Map.of("notificationId", "5", "handleStatus", "IGNORED"));

    assertEquals(expected, result.data());
    verify(notificationService).handle(5L, "IGNORED");
  }
}
