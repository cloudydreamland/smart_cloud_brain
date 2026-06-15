package com.smartcloudbrain.notification.websocket;

import com.smartcloudbrain.common.security.UserContextHeaders;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

  private final Map<Long, WebSocketSession> doctorSessions = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Long doctorId = resolveDoctorId(session);
    if (doctorId != null) {
      doctorSessions.put(doctorId, session);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    doctorSessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
  }

  public void sendToDoctor(Long doctorId, String payload) {
    if (doctorId == null) {
      return;
    }
    WebSocketSession session = doctorSessions.get(doctorId);
    if (session == null || !session.isOpen()) {
      return;
    }
    try {
      session.sendMessage(new TextMessage(payload));
    } catch (IOException ignored) {
      doctorSessions.remove(doctorId);
    }
  }

  private Long resolveDoctorId(WebSocketSession session) {
    String doctorId = session.getHandshakeHeaders().getFirst(UserContextHeaders.USER_ID);
    if (doctorId == null && session.getUri() != null && session.getUri().getQuery() != null) {
      for (String part : session.getUri().getQuery().split("&")) {
        String[] pair = part.split("=", 2);
        if (pair.length == 2 && "doctorId".equals(pair[0])) {
          doctorId = pair[1];
          break;
        }
      }
    }
    if (doctorId == null) {
      return null;
    }
    try {
      return Long.parseLong(doctorId);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}


