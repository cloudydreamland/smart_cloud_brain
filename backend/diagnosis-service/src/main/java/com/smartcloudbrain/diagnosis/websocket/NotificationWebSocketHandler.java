package com.smartcloudbrain.diagnosis.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

  private final Map<Long, WebSocketSession> doctorSessions = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Long doctorId = resolveDoctorId(session.getUri());
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

  private Long resolveDoctorId(URI uri) {
    if (uri == null) {
      return null;
    }
    String doctorId = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("doctorId");
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
