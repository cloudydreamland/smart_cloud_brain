package com.smartcloudbrain.doctor.controller;

import com.smartcloudbrain.common.result.Result;
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

  public InternalDoctorController(DoctorScheduleService doctorScheduleService) {
    this.doctorScheduleService = doctorScheduleService;
  }

  @GetMapping("/schedules/list")
  public Result<?> schedules() {
    return Result.success(doctorScheduleService.schedules());
  }

  @PostMapping("/schedules/publish")
  public Result<?> publishSchedules(@RequestBody InternalSchedulePublishRequest request) {
    return Result.success(doctorScheduleService.publishSchedules(request));
  }

  @GetMapping("/slots/list")
  public Result<?> slots() {
    return Result.success(doctorScheduleService.slots());
  }
}
