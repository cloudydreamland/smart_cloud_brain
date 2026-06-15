package com.smartcloudbrain.patient.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.patient.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @GetMapping("/info")
  public Result<?> info() {
    return Result.success(patientService.currentPatientInfo());
  }
}


