package com.smartcloudbrain.diagnosis.service;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

  public Map<String, Object> currentPatientInfo() {
    return Map.of("id", 1L, "name", "Patient Demo", "role", "PATIENT");
  }
}
