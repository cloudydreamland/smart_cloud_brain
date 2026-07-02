package com.smartcloudbrain.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.auth.dto.auth.LoginRequest;
import com.smartcloudbrain.auth.dto.auth.LoginResponse;
import com.smartcloudbrain.auth.dto.patient.EmailCodeSendRequest;
import com.smartcloudbrain.auth.dto.patient.PatientRegisterRequest;
import com.smartcloudbrain.auth.service.AuthService;
import com.smartcloudbrain.auth.service.PatientEmailVerificationService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthService authService;
  @Mock private PatientEmailVerificationService emailVerificationService;
  @Mock private CurrentUserService currentUserService;

  private AuthController controller() {
    return new AuthController(authService, emailVerificationService, currentUserService);
  }

  // ── register ────────────────────────────────────────────────────

  @Test
  void registerPatientReturnsSuccessResult() {
    PatientRegisterRequest request = new PatientRegisterRequest(
        "Alice", "13800000001", "alice@example.com", "123456", "123456", "FEMALE", 30, "", "");
    when(authService.registerPatient(any(PatientRegisterRequest.class)))
        .thenReturn(Map.of("patientId", 42L));

    Result<?> result = controller().registerPatient(request);

    assertEquals(0, result.code());
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) result.data();
    assertEquals(42L, data.get("patientId"));
    verify(authService).registerPatient(request);
  }

  // ── loginPatient ────────────────────────────────────────────────

  @Test
  void loginPatientReturnsToken() {
    LoginRequest request = new LoginRequest("13800000001", "123456");
    LoginResponse response = new LoginResponse("jwt-token", 1L, "PATIENT", "Alice");
    when(authService.loginPatient(request)).thenReturn(response);

    Result<?> result = controller().loginPatient(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
  }

  // ── loginDoctor ─────────────────────────────────────────────────

  @Test
  void loginDoctorReturnsToken() {
    LoginRequest request = new LoginRequest("13900000001", "123456");
    LoginResponse response = new LoginResponse("doc-jwt", 2L, "DOCTOR", "Dr. Chen");
    when(authService.loginDoctor(request)).thenReturn(response);

    Result<?> result = controller().loginDoctor(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
  }

  // ── loginAdmin ──────────────────────────────────────────────────

  @Test
  void loginAdminReturnsToken() {
    LoginRequest request = new LoginRequest("admin", "123456");
    LoginResponse response = new LoginResponse("admin-jwt", 3L, "ADMIN", "admin");
    when(authService.loginAdmin(request)).thenReturn(response);

    Result<?> result = controller().loginAdmin(request);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
  }

  // ── sendPatientEmailCode ────────────────────────────────────────

  @Test
  void sendPatientEmailCodeDelegatesToService() {
    EmailCodeSendRequest request = new EmailCodeSendRequest("alice@example.com", "13800000001", "register");
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    Map<String, Object> response = Map.of("sent", true);
    when(emailVerificationService.sendRegisterCode(eq(request), eq("10.0.0.1")))
        .thenReturn(response);

    Result<?> result = controller().sendPatientEmailCode(request, servletRequest);

    assertEquals(0, result.code());
    assertEquals(response, result.data());
  }

  @Test
  void sendPatientEmailCodeExtractsForwardedIp() {
    EmailCodeSendRequest request = new EmailCodeSendRequest("alice@example.com", "13800000001", "register");
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
    Map<String, Object> response = Map.of("sent", true);
    when(emailVerificationService.sendRegisterCode(eq(request), eq("192.168.1.1")))
        .thenReturn(response);

    Result<?> result = controller().sendPatientEmailCode(request, servletRequest);

    assertEquals(0, result.code());
  }

  // ── currentUser ─────────────────────────────────────────────────

  @Test
  void currentUserReturnsGatewayInjectedUserContext() {
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(7L, RoleType.DOCTOR, "doctor1"));

    Result<?> result = controller().currentUser();

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) result.data();
    assertEquals(0, result.code());
    assertEquals(7L, data.get("userId"));
    assertEquals("DOCTOR", data.get("role"));
    assertEquals("doctor1", data.get("name"));
  }
}
