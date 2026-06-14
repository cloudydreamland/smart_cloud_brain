package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.diagnosis.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.diagnosis.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prescription")
public class PrescriptionController {

  private final PrescriptionService prescriptionService;

  public PrescriptionController(PrescriptionService prescriptionService) {
    this.prescriptionService = prescriptionService;
  }

  @PostMapping("/check")
  public Result<?> check(@Valid @RequestBody PrescriptionCheckRequest request) {
    return Result.success(prescriptionService.check(request));
  }

  @PostMapping("/create")
  public Result<?> create(@Valid @RequestBody PrescriptionCreateRequest request) {
    return Result.success(prescriptionService.create(request));
  }

  @GetMapping("/list")
  public Result<?> list() {
    return Result.success(prescriptionService.list());
  }
}
