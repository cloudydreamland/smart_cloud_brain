package com.smartcloudbrain.ai.provider;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;

public interface AiProvider {

  TriageResponse triage(TriageRequest request);

  MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request);

  PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request);
}
