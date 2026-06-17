package com.smartcloudbrain.medicalrecord.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.medicalrecord.service.AiGatewayService;
import com.smartcloudbrain.medicalrecord.service.MedicalRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class MedicalRecordControllerSseTest {

  @Mock private AiGatewayService aiGatewayService;
  @Mock private MedicalRecordService medicalRecordService;

  @Test
  void streamGenerationRequiresDoctorRegistrationAndCallsAiService() {
    MedicalRecordController controller = new MedicalRecordController(aiGatewayService, medicalRecordService);
    when(aiGatewayService.generateMedicalRecord(any(MedicalRecordGenerateRequest.class)))
        .thenReturn(new MedicalRecordGenerateResponse(
            "chest pain",
            "two days",
            "",
            "stable",
            "observation",
            "follow up",
            false
        ));
    when(medicalRecordService.buildGenerateRequest(any(MedicalRecordGenerateRequest.class)))
        .thenReturn(new MedicalRecordGenerateRequest(42L, "CARDIOLOGY", "dialogue"));

    SseEmitter emitter = controller.generateStream(42L, "dialogue", "CARDIOLOGY");

    assertNotNull(emitter);
    verify(medicalRecordService).buildGenerateRequest(any(MedicalRecordGenerateRequest.class));
    verify(aiGatewayService, timeout(2500)).generateMedicalRecord(any(MedicalRecordGenerateRequest.class));
  }
}
