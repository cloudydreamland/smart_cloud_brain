package com.smartcloudbrain.patient.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.patient.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/patients")
public class InternalPatientController {

  private final PatientService patientService;

  public InternalPatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @GetMapping("/{patientId}/summary")
  public Result<?> summary(@PathVariable Long patientId) {
    return Result.success(patientService.patientSummary(patientId));
  }
}
