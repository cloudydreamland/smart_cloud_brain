package com.smartcloudbrain.doctor.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.doctor.dto.internal.InternalScheduleCancelRequest;
import com.smartcloudbrain.doctor.dto.internal.InternalSchedulePublishRequest;
import com.smartcloudbrain.doctor.dto.internal.InternalScheduleSaveRequest;
import com.smartcloudbrain.doctor.service.DoctorScheduleService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  public Result<?> schedules(
      @RequestParam(name = "startDate", required = false) LocalDate startDate,
      @RequestParam(name = "endDate", required = false) LocalDate endDate,
      @RequestParam(name = "departmentId", required = false) Long departmentId,
      @RequestParam(name = "doctorId", required = false) Long doctorId,
      @RequestParam(name = "status", required = false) String status
  ) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.schedules(startDate, endDate, departmentId, doctorId, status));
  }

  @PostMapping("/schedules/publish")
  public Result<?> publishSchedules(@RequestBody InternalSchedulePublishRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.publishSchedules(request));
  }

  @PostMapping("/schedules/save")
  public Result<?> saveSchedule(@RequestBody InternalScheduleSaveRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.saveSchedule(request));
  }

  @PostMapping("/schedules/cancel")
  public Result<?> cancelSchedule(@RequestBody InternalScheduleCancelRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.cancelSchedule(request));
  }

  @GetMapping("/slots/list")
  public Result<?> slots() {
    internalRequestGuard.requireServiceRequest();
    return Result.success(doctorScheduleService.slots());
  }
}
