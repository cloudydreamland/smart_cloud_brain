package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.diagnosis.service.AiGatewayService;
import com.smartcloudbrain.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

  private final AiGatewayService aiGatewayService;

  public TriageController(AiGatewayService aiGatewayService) {
    this.aiGatewayService = aiGatewayService;
  }

  @PostMapping("/consult")
  public Result<?> consult(@Valid @RequestBody TriageRequest request) {
    return Result.success(aiGatewayService.triage(request));
  }
}
