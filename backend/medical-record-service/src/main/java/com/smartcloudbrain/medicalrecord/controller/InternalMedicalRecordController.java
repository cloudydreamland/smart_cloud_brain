package com.smartcloudbrain.medicalrecord.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.medicalrecord.service.MedicalRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/medical-records")
public class InternalMedicalRecordController {

  private final MedicalRecordService medicalRecordService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalMedicalRecordController(MedicalRecordService medicalRecordService, InternalRequestGuard internalRequestGuard) {
    this.medicalRecordService = medicalRecordService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @GetMapping("/patient/{patientId}")
  public Result<?> byPatient(@PathVariable("patientId") Long patientId) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(medicalRecordService.recordsByPatient(patientId));
  }
}
