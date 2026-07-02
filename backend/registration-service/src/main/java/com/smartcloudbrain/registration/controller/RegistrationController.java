package com.smartcloudbrain.registration.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.registration.dto.registration.CreateRegistrationRequest;
import com.smartcloudbrain.registration.service.RegistrationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
  public Result<?> create(
      @Valid @RequestBody CreateRegistrationRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
  ) {
    return Result.success(registrationService.create(request, idempotencyKey));
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

  @PostMapping("/pay")
  public Result<?> pay(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.pay(request.get("registrationId")));
  }

  @PostMapping("/check-in")
  public Result<?> checkIn(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.checkIn(request.get("registrationId")));
  }

  @PostMapping("/join-queue")
  public Result<?> joinQueue(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.joinQueue(request.get("registrationId")));
  }

  @PostMapping("/call")
  public Result<?> call(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.call(request.get("registrationId")));
  }

  @PostMapping("/start-consultation")
  public Result<?> startConsultation(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.startConsultation(request.get("registrationId")));
  }

  @PostMapping("/refund")
  public Result<?> refund(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.refund(request.get("registrationId")));
  }

  @PostMapping("/complete")
  public Result<?> complete(@RequestBody Map<String, Long> request) {
    return Result.success(registrationService.complete(request.get("registrationId")));
  }
}


