package com.smartcloudbrain.medicalrecord.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.medicalrecord.service.MedicalRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/medical-records")
public class InternalMedicalRecordController {

  private final MedicalRecordService medicalRecordService;

  public InternalMedicalRecordController(MedicalRecordService medicalRecordService) {
    this.medicalRecordService = medicalRecordService;
  }

  @GetMapping("/patient/{patientId}")
  public Result<?> byPatient(@PathVariable Long patientId) {
    return Result.success(medicalRecordService.recordsByPatient(patientId));
  }
}
