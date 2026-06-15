package com.smartcloudbrain.registration.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.registration.dto.registration.CreateRegistrationRequest;
import com.smartcloudbrain.registration.service.RegistrationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

  private final RegistrationService registrationService;

  public RegistrationController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @PostMapping("/create")
  public Result<?> create(@Valid @RequestBody CreateRegistrationRequest request) {
    return Result.success(registrationService.create(request));
  }

  @GetMapping("/list")
  public Result<?> list() {
    return Result.success(registrationService.list());
  }

  @GetMapping("/slots")
  public Result<?> slots() {
    return Result.success(registrationService.slots());
  }

  @PostMapping("/cancel")
  public Result<?> cancel(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.cancel(request.get("registrationId")));
  }
}


