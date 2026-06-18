package com.smartcloudbrain.auth.controller;

import com.smartcloudbrain.auth.service.AuthService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.common.security.JwtClaims;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

  private final AuthService authService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalAuthController(AuthService authService, InternalRequestGuard internalRequestGuard) {
    this.authService = authService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @PostMapping("/verify")
  public Result<JwtClaims> verify(@RequestHeader("Authorization") String authorization) {
    internalRequestGuard.requireServiceRequest();
    String token = authorization == null ? "" : authorization.replaceFirst("^Bearer\\s+", "");
    return Result.success(authService.verify(token));
  }
}

