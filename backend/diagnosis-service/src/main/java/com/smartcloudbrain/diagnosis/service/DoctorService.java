package com.smartcloudbrain.diagnosis.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DoctorService {

  public List<Map<String, Object>> listDoctors(Long departmentId) {
    return List.of(Map.of("id", 1L, "name", "Doctor Demo", "departmentId", departmentId == null ? 1L : departmentId));
  }

  public Map<String, Object> doctorDetail(Long id) {
    return Map.of("id", id, "name", "Doctor Demo", "title", "Chief Physician");
  }
}
