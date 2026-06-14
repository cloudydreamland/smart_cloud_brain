package com.smartcloudbrain.ai.application;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestrationService {

  private final AiProvider aiProvider;
  private final AiTaskLogService aiTaskLogService;

  public AiOrchestrationService(AiProvider aiProvider, AiTaskLogService aiTaskLogService) {
    this.aiProvider = aiProvider;
    this.aiTaskLogService = aiTaskLogService;
  }

  public TriageResponse triage(TriageRequest request) {
    TriageResponse response = aiProvider.triage(request);
    aiTaskLogService.recordSuccess("TRIAGE");
    return response;
  }

  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    MedicalRecordGenerateResponse response = aiProvider.generateMedicalRecord(request);
    aiTaskLogService.recordSuccess("MEDICAL_RECORD");
    return response;
  }

  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    PrescriptionCheckResponse response = aiProvider.checkPrescription(request);
    aiTaskLogService.recordSuccess("PRESCRIPTION_CHECK");
    return response;
  }
}
