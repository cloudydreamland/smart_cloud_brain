package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.diagnosis.dto.auth.LoginRequest;
import com.smartcloudbrain.diagnosis.dto.auth.LoginResponse;
import com.smartcloudbrain.diagnosis.dto.patient.PatientRegisterRequest;
import com.smartcloudbrain.diagnosis.security.JwtTokenProvider;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  public Map<String, Long> registerPatient(PatientRegisterRequest request) {
    return Map.of("patientId", 1L);
  }

  public LoginResponse loginPatient(LoginRequest request) {
    return new LoginResponse(jwtTokenProvider.issueToken(1L, RoleType.PATIENT), 1L, RoleType.PATIENT.name(), "Patient Demo");
  }

  public LoginResponse loginDoctor(LoginRequest request) {
    return new LoginResponse(jwtTokenProvider.issueToken(1L, RoleType.DOCTOR), 1L, RoleType.DOCTOR.name(), "Doctor Demo");
  }

  public LoginResponse loginAdmin(LoginRequest request) {
    return new LoginResponse(jwtTokenProvider.issueToken(1L, RoleType.ADMIN), 1L, RoleType.ADMIN.name(), "Admin Demo");
  }
}
