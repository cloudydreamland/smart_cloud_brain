package com.smartcloudbrain.ai.provider;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;

public interface AiProvider {

  default String providerName() {
    return getClass().getSimpleName();
  }

  default String modelName() {
    return "";
  }

  default TriageResponse triage(TriageRequest request) {
    return triage(request, null);
  }

  TriageResponse triage(TriageRequest request, PromptResolveResponse prompt);

  default MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    return generateMedicalRecord(request, null);
  }

  MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request, PromptResolveResponse prompt);

  default PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    return checkPrescription(request, null);
  }

  PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request, PromptResolveResponse prompt);
}
