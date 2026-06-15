package com.smartcloudbrain.doctor.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.doctor.service.DoctorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

  private final DoctorService doctorService;

  public DoctorController(DoctorService doctorService) {
    this.doctorService = doctorService;
  }

  @GetMapping("/list")
  public Result<?> list(@RequestParam(required = false) Long departmentId) {
    return Result.success(doctorService.listDoctors(departmentId));
  }

  @GetMapping("/department/list")
  public Result<?> departments() {
    return Result.success(doctorService.departments());
  }

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam Long id) {
    return Result.success(doctorService.doctorDetail(id));
  }
}


