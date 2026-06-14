package com.smartcloudbrain.diagnosis.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

  public List<Map<String, Object>> list(String readStatus) {
    return List.of();
  }

  public Map<String, Object> markRead(Long notificationId) {
    return Map.of("notificationId", notificationId, "readStatus", "READ");
  }
}
