package com.smartcloudbrain.patient.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.patient.service.PatientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/patients")
public class InternalPatientController {

  private final PatientService patientService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalPatientController(PatientService patientService, InternalRequestGuard internalRequestGuard) {
    this.patientService = patientService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @GetMapping("/{patientId}/summary")
  public Result<?> summary(@PathVariable("patientId") Long patientId) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(patientService.patientSummary(patientId));
  }
}
