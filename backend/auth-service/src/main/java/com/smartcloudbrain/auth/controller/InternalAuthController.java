package com.smartcloudbrain.auth.controller;

import com.smartcloudbrain.auth.service.AuthService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.JwtClaims;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

  private final AuthService authService;

  public InternalAuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/verify")
  public Result<JwtClaims> verify(@RequestHeader("Authorization") String authorization) {
    String token = authorization == null ? "" : authorization.replaceFirst("^Bearer\\s+", "");
    return Result.success(authService.verify(token));
  }
}

