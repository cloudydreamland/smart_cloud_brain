package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.diagnosis.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.DrugSaveRequest;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminCatalogService {

  public List<Map<String, Object>> departments() {
    return List.of(Map.of("id", 1L, "code", "CARDIOLOGY", "name", "Cardiology"));
  }

  public Map<String, Object> saveDepartment(DepartmentSaveRequest request) {
    return Map.of("departmentId", request.id() == null ? 1L : request.id());
  }

  public Map<String, Object> saveDoctor(DoctorSaveRequest request) {
    return Map.of("doctorId", request.id() == null ? 1L : request.id());
  }

  public List<Map<String, Object>> drugs() {
    return List.of(Map.of("id", 1L, "name", "Aspirin"));
  }

  public Map<String, Object> saveDrug(DrugSaveRequest request) {
    return Map.of("drugId", request.id() == null ? 1L : request.id());
  }
}
