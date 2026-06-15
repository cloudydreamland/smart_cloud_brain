package com.smartcloudbrain.medicalrecord.service;

import com.smartcloudbrain.medicalrecord.client.AiServiceClient;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayService {

  private final AiServiceClient aiServiceClient;

  public AiGatewayService(AiServiceClient aiServiceClient) {
    this.aiServiceClient = aiServiceClient;
  }

  public TriageResponse triage(TriageRequest request) {
    try {
      return aiServiceClient.triage(request).data();
    } catch (RuntimeException ex) {
      return new TriageResponse("General Practice", "GENERAL", List.of(), "AI service is unavailable. Manual triage is recommended.", true);
    }
  }

  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    try {
      return aiServiceClient.generateMedicalRecord(request).data();
    } catch (RuntimeException ex) {
      return new MedicalRecordGenerateResponse("", "", "", "", "Manual input required", "AI service is unavailable. Please complete the record manually.", true);
    }
  }

  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    try {
      return aiServiceClient.checkPrescription(request).data();
    } catch (RuntimeException ex) {
      return new PrescriptionCheckResponse("UNKNOWN", "AI review is unavailable. Doctor manual review is required before saving.", List.of(), true);
    }
  }

  public PromptResolveResponse resolvePrompt(PromptResolveRequest request) {
    try {
      return aiServiceClient.resolvePrompt(request).data();
    } catch (RuntimeException ex) {
      return new PromptResolveResponse(request.taskType(), request.departmentCode(), "fallback", "AI prompt service unavailable.", "{}", "fallback");
    }
  }
}


