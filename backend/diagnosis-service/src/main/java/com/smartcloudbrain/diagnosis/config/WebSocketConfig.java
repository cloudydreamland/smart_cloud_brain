package com.smartcloudbrain.diagnosis.config;

import com.smartcloudbrain.diagnosis.websocket.NotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final NotificationWebSocketHandler notificationWebSocketHandler;

  public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler) {
    this.notificationWebSocketHandler = notificationWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
        .setAllowedOriginPatterns("*");
  }
}
