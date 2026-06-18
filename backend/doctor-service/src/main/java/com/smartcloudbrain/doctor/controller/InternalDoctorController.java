package com.smartcloudbrain.doctor.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.doctor.dto.internal.InternalSchedulePublishRequest;
import com.smartcloudbrain.doctor.service.DoctorScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/doctors")
public class InternalDoctorController {

  private final DoctorScheduleService doctorScheduleService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalDoctorController(DoctorScheduleService doctorScheduleService, InternalRequestGuard internalRequestGuard) {
    this.doctorScheduleService = doctorScheduleService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @GetMapping("/schedules/list")
  public Result<?> schedules() {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.schedules());
  }

  @PostMapping("/schedules/publish")
  public Result<?> publishSchedules(@RequestBody InternalSchedulePublishRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.publishSchedules(request));
  }

  @GetMapping("/slots/list")
  public Result<?> slots() {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.slots());
  }
}
