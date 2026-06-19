package com.smartcloudbrain.auth.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.auth.dto.auth.LoginRequest;
import com.smartcloudbrain.auth.dto.patient.PatientRegisterRequest;
import com.smartcloudbrain.auth.service.AuthService;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

  private final AuthService authService;
  private final CurrentUserService currentUserService;

  public AuthController(AuthService authService, CurrentUserService currentUserService) {
    this.authService = authService;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/patient/register")
  public Result<?> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
    return Result.success(authService.registerPatient(request));
  }

  @PostMapping("/patient/login")
  public Result<?> loginPatient(@Valid @RequestBody LoginRequest request) {
    return Result.success(authService.loginPatient(request));
  }

  @PostMapping("/doctor/login")
  public Result<?> loginDoctor(@Valid @RequestBody LoginRequest request) {
    return Result.success(authService.loginDoctor(request));
  }

  @PostMapping("/admin/login")
  public Result<?> loginAdmin(@Valid @RequestBody LoginRequest request) {
    return Result.success(authService.loginAdmin(request));
  }

  @GetMapping("/auth/me")
  public Result<?> currentUser() {
    AuthenticatedUser user = currentUserService.get();
    return Result.success(Map.of(
        "userId", user.userId(),
        "role", user.role().name(),
        "name", user.name()
    ));
  }
}


