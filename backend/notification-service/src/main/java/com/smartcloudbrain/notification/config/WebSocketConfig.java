package com.smartcloudbrain.notification.config;

import com.smartcloudbrain.notification.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final NotificationWebSocketHandler notificationWebSocketHandler;
  private final String allowedOriginPatterns;

  public WebSocketConfig(
      NotificationWebSocketHandler notificationWebSocketHandler,
      @Value("${websocket.allowed-origin-patterns:http://localhost:5174,http://localhost}") String allowedOriginPatterns
  ) {
    this.notificationWebSocketHandler = notificationWebSocketHandler;
    this.allowedOriginPatterns = allowedOriginPatterns;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
        .setAllowedOriginPatterns(allowedOriginPatterns.split("\\s*,\\s*"));
  }
}


