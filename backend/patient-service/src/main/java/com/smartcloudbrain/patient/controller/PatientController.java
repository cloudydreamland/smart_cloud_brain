package com.smartcloudbrain.patient.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.patient.dto.PatientProfileSaveRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorDeleteRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorSaveRequest;
import com.smartcloudbrain.patient.service.PatientCatalogService;
import com.smartcloudbrain.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class PatientController {

  private final PatientService patientService;
  private final PatientCatalogService patientCatalogService;

  public PatientController(PatientService patientService, PatientCatalogService patientCatalogService) {
    this.patientService = patientService;
    this.patientCatalogService = patientCatalogService;
  }

  @GetMapping("/info")
  public Result<?> info() {
    return Result.success(patientService.currentPatientInfo());
  }

  @PostMapping("/profile/save")
  public Result<?> saveProfile(@Valid @RequestBody PatientProfileSaveRequest request) {
    return Result.success(patientService.saveProfile(request));
  }

  @GetMapping("/visitor/list")
  public Result<?> visitors() {
    return Result.success(patientService.visitors());
  }

  @PostMapping("/visitor/save")
  public Result<?> saveVisitor(@Valid @RequestBody PatientVisitorSaveRequest request) {
    return Result.success(patientService.saveVisitor(request));
  }

  @PostMapping("/visitor/delete")
  public Result<?> deleteVisitor(@Valid @RequestBody PatientVisitorDeleteRequest request) {
    return Result.success(patientService.deleteVisitor(request));
  }

  @GetMapping("/departments")
  public Result<?> departments() {
    return Result.success(patientCatalogService.departments());
  }

  @GetMapping("/doctors")
  public Result<?> doctors(@RequestParam(name = "departmentId", required = false) Long departmentId) {
    return Result.success(patientCatalogService.doctors(departmentId));
  }
}


