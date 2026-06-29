package com.smartcloudbrain.auth.service;

import com.smartcloudbrain.auth.dto.auth.LoginRequest;
import com.smartcloudbrain.auth.dto.auth.LoginResponse;
import com.smartcloudbrain.auth.dto.patient.PatientRegisterRequest;
import com.smartcloudbrain.auth.entity.AdminUser;
import com.smartcloudbrain.auth.entity.Doctor;
import com.smartcloudbrain.auth.entity.Patient;
import com.smartcloudbrain.auth.repository.AdminUserRepository;
import com.smartcloudbrain.auth.repository.DoctorRepository;
import com.smartcloudbrain.auth.repository.PatientRepository;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.JwtClaims;
import com.smartcloudbrain.common.security.JwtService;
import com.smartcloudbrain.common.security.RoleType;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final JwtService jwtService;
  private final PasswordService passwordService;
  private final PatientRepository patientRepository;
  private final DoctorRepository doctorRepository;
  private final AdminUserRepository adminUserRepository;
  
  @Value("${auth.enable-demo-account:false}")
  private boolean enableDemoAccount;

  public AuthService(
      JwtService jwtService,
      PasswordService passwordService,
      PatientRepository patientRepository,
      DoctorRepository doctorRepository,
      AdminUserRepository adminUserRepository
  ) {
    this.jwtService = jwtService;
    this.passwordService = passwordService;
    this.patientRepository = patientRepository;
    this.doctorRepository = doctorRepository;
    this.adminUserRepository = adminUserRepository;
  }

  @Transactional
  public Map<String, Long> registerPatient(PatientRegisterRequest request) {
    patientRepository.findByPhone(request.phone()).ifPresent(patient -> {
      throw new BusinessException(ErrorCode.CONFLICT);
    });
    Patient patient = new Patient();
    patient.setName(request.name());
    patient.setPhone(request.phone());
    patient.setPasswordHash(passwordService.encode(request.password()));
    patient.setGender(request.gender());
    patient.setAge(request.age());
    patient.setAllergyHistory(request.allergyHistory());
    patient.setPastHistory(request.pastHistory());
    patient.setUpdatedAt(LocalDateTime.now());
    return Map.of("patientId", patientRepository.save(patient).getId());
  }

  @Transactional
  public LoginResponse loginPatient(LoginRequest request) {
    Patient patient = patientRepository.findByPhone(request.account())
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    passwordService.assertMatches(request.password(), patient.getPasswordHash());
    if (passwordService.requiresUpgrade(patient.getPasswordHash())) {
      patient.setPasswordHash(passwordService.encode(request.password()));
      patient.setUpdatedAt(LocalDateTime.now());
      patientRepository.save(patient);
    }
    return session(patient.getId(), RoleType.PATIENT, patient.getName());
  }

  @Transactional
  public LoginResponse loginDoctor(LoginRequest request) {
    Doctor doctor = doctorRepository.findByPhone(request.account())
        .or(() -> doctorByDemoAccount(request.account()))
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    if ("DISABLED".equalsIgnoreCase(doctor.getStatus())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    passwordService.assertMatches(request.password(), doctor.getPasswordHash());
    if (passwordService.requiresUpgrade(doctor.getPasswordHash())) {
      doctor.setPasswordHash(passwordService.encode(request.password()));
      doctorRepository.save(doctor);
    }
    return session(doctor.getId(), RoleType.DOCTOR, doctor.getName());
  }

  @Transactional
  public LoginResponse loginAdmin(LoginRequest request) {
    AdminUser admin = adminUserRepository.findByUsername(request.account())
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    if ("DISABLED".equalsIgnoreCase(admin.getStatus())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    passwordService.assertMatches(request.password(), admin.getPasswordHash());
    if (passwordService.requiresUpgrade(admin.getPasswordHash())) {
      admin.setPasswordHash(passwordService.encode(request.password()));
      admin.setUpdatedAt(LocalDateTime.now());
      adminUserRepository.save(admin);
    }
    return session(admin.getId(), RoleType.ADMIN, admin.getName());
  }

  public JwtClaims verify(String token) {
    return jwtService.verify(token);
  }

  private LoginResponse session(Long id, RoleType role, String name) {
    return new LoginResponse(jwtService.issue(id, role, name), id, role.name(), name);
  }

  private java.util.Optional<Doctor> doctorByDemoAccount(String account) {
    if (!enableDemoAccount) {
      return java.util.Optional.empty();
    }
    if (account == null || !account.matches("doctor\\d+")) {
      return java.util.Optional.empty();
    }
    return doctorRepository.findById(Long.valueOf(account.substring("doctor".length())));
  }
}

