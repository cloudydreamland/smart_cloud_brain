package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.diagnosis.service.AiGatewayService;
import com.smartcloudbrain.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompt-template")
public class PromptTemplateController {

  private final AiGatewayService aiGatewayService;

  public PromptTemplateController(AiGatewayService aiGatewayService) {
    this.aiGatewayService = aiGatewayService;
  }

  @GetMapping("/list")
  public Result<?> list(@RequestParam String taskType, @RequestParam(required = false) String departmentCode) {
    return Result.success(aiGatewayService.resolvePrompt(new PromptResolveRequest(taskType, departmentCode)));
  }
}
