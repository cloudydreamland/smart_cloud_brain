package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.diagnosis.service.AiGatewayService;
import com.smartcloudbrain.diagnosis.websocket.NotificationWebSocketHandler;
import com.smartcloudbrain.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prescription")
public class PrescriptionController {

  private final AiGatewayService aiGatewayService;
  private final NotificationWebSocketHandler notificationWebSocketHandler;

  public PrescriptionController(AiGatewayService aiGatewayService, NotificationWebSocketHandler notificationWebSocketHandler) {
    this.aiGatewayService = aiGatewayService;
    this.notificationWebSocketHandler = notificationWebSocketHandler;
  }

  @PostMapping("/check")
  public Result<?> check(@Valid @RequestBody PrescriptionCheckRequest request) {
    PrescriptionCheckResponse response = aiGatewayService.checkPrescription(request);
    if ("HIGH".equals(response.riskLevel()) || "MEDIUM".equals(response.riskLevel())) {
      notificationWebSocketHandler.sendToDoctor(request.doctorId(), """
          {"type":"PRESCRIPTION_HIGH_RISK","riskLevel":"%s","title":"High risk prescription alert","content":"%s"}
          """.formatted(response.riskLevel(), response.suggestions()));
    }
    return Result.success(response);
  }
}
