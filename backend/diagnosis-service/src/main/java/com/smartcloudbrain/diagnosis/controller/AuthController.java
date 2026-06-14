package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.diagnosis.dto.auth.LoginRequest;
import com.smartcloudbrain.diagnosis.dto.patient.PatientRegisterRequest;
import com.smartcloudbrain.diagnosis.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
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
}
