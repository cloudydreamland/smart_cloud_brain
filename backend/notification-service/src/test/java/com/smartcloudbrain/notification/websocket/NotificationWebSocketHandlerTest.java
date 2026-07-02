package com.smartcloudbrain.notification.websocket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.common.security.UserContextHeaders;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class NotificationWebSocketHandlerTest {

  private NotificationWebSocketHandler handler;

  @BeforeEach
  void setUp() {
    handler = new NotificationWebSocketHandler();
  }

  private WebSocketSession mockSession(String userId, String role, String sessionId) {
    WebSocketSession session = mock(WebSocketSession.class);
    HttpHeaders headers = new HttpHeaders();
    if (userId != null) headers.add(UserContextHeaders.USER_ID, userId);
    if (role != null) headers.add(UserContextHeaders.USER_ROLE, role);
    when(session.getHandshakeHeaders()).thenReturn(headers);
    lenient().when(session.getId()).thenReturn(sessionId);
    lenient().when(session.isOpen()).thenReturn(true);
    return session;
  }

  @Test
  void afterConnectionEstablishedRegistersDoctorSession() throws IOException {
    WebSocketSession session = mockSession("8", "DOCTOR", "s1");

    handler.afterConnectionEstablished(session);

    handler.sendToDoctor(8L, "{\"test\":true}");
    verify(session).sendMessage(new TextMessage("{\"test\":true}"));
  }

  @Test
  void afterConnectionEstablishedRejectsNonDoctorRole() throws IOException {
    WebSocketSession session = mockSession("8", "PATIENT", "s1");

    handler.afterConnectionEstablished(session);

    verify(session).close(CloseStatus.POLICY_VIOLATION);
  }

  @Test
  void afterConnectionEstablishedRejectsNullDoctorId() throws IOException {
    WebSocketSession session = mockSession(null, "DOCTOR", "s1");

    handler.afterConnectionEstablished(session);

    verify(session).close(CloseStatus.POLICY_VIOLATION);
  }

  @Test
  void afterConnectionEstablishedRejectsInvalidDoctorId() throws IOException {
    WebSocketSession session = mockSession("not-a-number", "DOCTOR", "s1");

    handler.afterConnectionEstablished(session);

    verify(session).close(CloseStatus.POLICY_VIOLATION);
  }

  @Test
  void afterConnectionClosedRemovesSession() throws IOException {
    WebSocketSession session = mockSession("8", "DOCTOR", "s1");
    handler.afterConnectionEstablished(session);

    handler.afterConnectionClosed(session, CloseStatus.NORMAL);

    // After removal, sendToDoctor to same doctorId is a no-op (no session registered)
    handler.sendToDoctor(8L, "payload");
    verify(session, never()).sendMessage(any());
  }

  @Test
  void sendToDoctorWithNullDoctorIdIsNoOp() {
    assertDoesNotThrow(() -> handler.sendToDoctor(null, "payload"));
  }

  @Test
  void sendToDoctorWithNoSessionIsNoOp() {
    assertDoesNotThrow(() -> handler.sendToDoctor(999L, "payload"));
  }

  @Test
  void sendToDoctorWithClosedSessionIsNoOp() throws IOException {
    WebSocketSession session = mockSession("8", "DOCTOR", "s1");
    handler.afterConnectionEstablished(session);
    when(session.isOpen()).thenReturn(false);

    handler.sendToDoctor(8L, "payload");

    verify(session, never()).sendMessage(any());
  }

  @Test
  void sendToDoctorHandlesIOException() throws IOException {
    WebSocketSession session = mockSession("8", "DOCTOR", "s1");
    handler.afterConnectionEstablished(session);
    org.mockito.Mockito.doThrow(new IOException("connection lost"))
        .when(session).sendMessage(any(TextMessage.class));

    handler.sendToDoctor(8L, "payload");

    // Second call should be a no-op since session was removed on IOException
    handler.sendToDoctor(8L, "payload2");
  }

  @Test
  void multipleDoctorsCanConnect() throws IOException {
    WebSocketSession session1 = mockSession("1", "DOCTOR", "s1");
    WebSocketSession session2 = mockSession("2", "DOCTOR", "s2");

    handler.afterConnectionEstablished(session1);
    handler.afterConnectionEstablished(session2);

    handler.sendToDoctor(1L, "msg1");
    handler.sendToDoctor(2L, "msg2");

    verify(session1).sendMessage(new TextMessage("msg1"));
    verify(session2).sendMessage(new TextMessage("msg2"));
  }
}
