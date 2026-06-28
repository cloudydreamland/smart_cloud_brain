package com.smartcloudbrain.doctor.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.doctor.service.DoctorService;
import com.smartcloudbrain.doctor.service.DoctorScheduleService;
import com.smartcloudbrain.doctor.service.DoctorWorkspaceService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

  private final DoctorService doctorService;
  private final DoctorScheduleService doctorScheduleService;
  private final DoctorWorkspaceService doctorWorkspaceService;
  private final CurrentUserService currentUserService;

  public DoctorController(
      DoctorService doctorService,
      DoctorScheduleService doctorScheduleService,
      DoctorWorkspaceService doctorWorkspaceService,
      CurrentUserService currentUserService
  ) {
    this.doctorService = doctorService;
    this.doctorScheduleService = doctorScheduleService;
    this.doctorWorkspaceService = doctorWorkspaceService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/list")
  public Result<?> list(@RequestParam(name = "departmentId", required = false) Long departmentId) {
    return Result.success(doctorService.listDoctors(departmentId));
  }

  @GetMapping("/department/list")
  public Result<?> departments() {
    return Result.success(doctorService.departments());
  }

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam("id") Long id) {
    return Result.success(doctorService.doctorDetail(id));
  }

  @GetMapping("/schedule/list")
  public Result<?> mySchedules(
      @RequestParam(name = "startDate", required = false) LocalDate startDate,
      @RequestParam(name = "endDate", required = false) LocalDate endDate,
      @RequestParam(name = "status", required = false) String status
  ) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    return Result.success(doctorScheduleService.schedules(startDate, endDate, null, user.userId(), status));
  }

  @GetMapping("/dashboard")
  public Result<?> dashboard() {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    return Result.success(doctorWorkspaceService.dashboard(user.userId()));
  }

  @GetMapping("/queue")
  public Result<?> queue() {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    return Result.success(doctorWorkspaceService.queue(user.userId()));
  }
}


