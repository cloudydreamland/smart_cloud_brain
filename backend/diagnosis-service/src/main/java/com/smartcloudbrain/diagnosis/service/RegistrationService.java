package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.diagnosis.dto.registration.CreateRegistrationRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

  public Map<String, Object> create(CreateRegistrationRequest request) {
    return Map.of("registrationId", 100L, "status", "CREATED");
  }

  public List<Map<String, Object>> list() {
    return List.of();
  }

  public Map<String, Object> cancel(Long registrationId) {
    return Map.of("registrationId", registrationId, "status", "CANCELLED");
  }
}
