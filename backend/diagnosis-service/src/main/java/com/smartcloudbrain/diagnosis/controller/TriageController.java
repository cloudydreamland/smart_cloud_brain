package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.diagnosis.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

  private final TriageService triageService;

  public TriageController(TriageService triageService) {
    this.triageService = triageService;
  }

  @PostMapping("/consult")
  public Result<?> consult(@Valid @RequestBody TriageRequest request) {
    return Result.success(triageService.consult(request));
  }

  @GetMapping("/list")
  public Result<?> list() {
    return Result.success(triageService.list());
  }
}
