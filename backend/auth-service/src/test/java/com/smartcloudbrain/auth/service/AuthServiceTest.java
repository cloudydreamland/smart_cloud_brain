package com.smartcloudbrain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.auth.dto.auth.LoginRequest;
import com.smartcloudbrain.auth.dto.auth.LoginResponse;
import com.smartcloudbrain.auth.dto.patient.PatientRegisterRequest;
import com.smartcloudbrain.auth.entity.AdminUser;
import com.smartcloudbrain.auth.entity.Doctor;
import com.smartcloudbrain.auth.entity.Patient;
import com.smartcloudbrain.auth.repository.AdminUserRepository;
import com.smartcloudbrain.auth.repository.DoctorRepository;
import com.smartcloudbrain.auth.repository.PatientRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.JwtService;
import com.smartcloudbrain.common.security.RoleType;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private JwtService jwtService;
  @Mock private PasswordService passwordService;
  @Mock private PatientRepository patientRepository;
  @Mock private DoctorRepository doctorRepository;
  @Mock private AdminUserRepository adminUserRepository;
  @InjectMocks private AuthService authService;

  @Test
  void registersNewPatient() {
    Patient saved = new Patient();
    saved.setId(9L);
    when(patientRepository.findByPhone("13800000001")).thenReturn(Optional.empty());
    when(passwordService.encode("123456")).thenReturn("encoded");
    when(patientRepository.save(any(Patient.class))).thenReturn(saved);

    Map<String, Long> result = authService.registerPatient(new PatientRegisterRequest(
        "Alice", "13800000001", "123456", "FEMALE", 30, "", ""));

    assertEquals(9L, result.get("patientId"));
  }

  @Test
  void rejectsDuplicatePatientPhone() {
    when(patientRepository.findByPhone("13800000001")).thenReturn(Optional.of(new Patient()));

    assertThrows(BusinessException.class, () -> authService.registerPatient(new PatientRegisterRequest(
        "Alice", "13800000001", "123456", "FEMALE", 30, "", "")));
    verify(patientRepository, never()).save(any());
  }

  @Test
  void logsInPatientAndUpgradesLegacyPassword() {
    Patient patient = new Patient();
    patient.setId(1L);
    patient.setName("patient");
    patient.setPasswordHash("{plain}123456");
    when(patientRepository.findByPhone("13800000001")).thenReturn(Optional.of(patient));
    when(passwordService.requiresUpgrade("{plain}123456")).thenReturn(true);
    when(passwordService.encode("123456")).thenReturn("{bcrypt}encoded");
    when(jwtService.issue(1L, RoleType.PATIENT, "patient")).thenReturn("jwt");

    LoginResponse response = authService.loginPatient(new LoginRequest("13800000001", "123456"));

    assertEquals("jwt", response.token());
    assertEquals("PATIENT", response.role());
    verify(patientRepository).save(patient);
  }

  @Test
  void logsInDoctorByDemoAccount() {
    Doctor doctor = new Doctor();
    doctor.setId(1L);
    doctor.setName("doctor1");
    doctor.setPasswordHash("{bcrypt}hash");
    when(doctorRepository.findByPhone("doctor1")).thenReturn(Optional.empty());
    when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
    when(jwtService.issue(1L, RoleType.DOCTOR, "doctor1")).thenReturn("doctor.jwt");

    LoginResponse response = authService.loginDoctor(new LoginRequest("doctor1", "123456"));

    assertEquals("doctor.jwt", response.token());
    assertEquals("DOCTOR", response.role());
  }

  @Test
  void logsInAdmin() {
    AdminUser admin = new AdminUser();
    admin.setId(3L);
    admin.setName("admin");
    admin.setPasswordHash("{bcrypt}hash");
    when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(jwtService.issue(3L, RoleType.ADMIN, "admin")).thenReturn("admin.jwt");

    LoginResponse response = authService.loginAdmin(new LoginRequest("admin", "123456"));

    assertEquals("admin.jwt", response.token());
    assertEquals("ADMIN", response.role());
  }

  @Test
  void rejectsUnknownLogin() {
    when(patientRepository.findByPhone("missing")).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> authService.loginPatient(new LoginRequest("missing", "bad")));
  }
}
