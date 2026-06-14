package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.diagnosis.service.DoctorService;
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

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam Long id) {
    return Result.success(doctorService.doctorDetail(id));
  }
}
