package com.smartcloudbrain.medicalrecord.client;

import com.smartcloudbrain.aiapi.constant.AiInternalApi;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", url = "${services.ai.base-url}")
public interface AiServiceClient {

  @PostMapping(AiInternalApi.TRIAGE)
  Result<TriageResponse> triage(@RequestBody TriageRequest request);

  @PostMapping(AiInternalApi.MEDICAL_RECORD_GENERATE)
  Result<MedicalRecordGenerateResponse> generateMedicalRecord(@RequestBody MedicalRecordGenerateRequest request);

  @PostMapping(AiInternalApi.PRESCRIPTION_CHECK)
  Result<PrescriptionCheckResponse> checkPrescription(@RequestBody PrescriptionCheckRequest request);

  @PostMapping(AiInternalApi.PROMPT_RESOLVE)
  Result<PromptResolveResponse> resolvePrompt(@RequestBody PromptResolveRequest request);
}


