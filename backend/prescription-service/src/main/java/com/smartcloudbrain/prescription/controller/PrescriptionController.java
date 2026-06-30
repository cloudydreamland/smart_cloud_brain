package com.smartcloudbrain.prescription.controller;

import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.prescription.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping("/drug/list")
  public Result<?> drugs() {
    return Result.success(prescriptionService.availableDrugs());
  }

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam("id") Long id) {
    return Result.success(prescriptionService.detail(id));
  }

  @GetMapping("/{id}")
  public Result<?> detailByPath(@PathVariable Long id) {
    return Result.success(prescriptionService.detail(id));
  }
}

