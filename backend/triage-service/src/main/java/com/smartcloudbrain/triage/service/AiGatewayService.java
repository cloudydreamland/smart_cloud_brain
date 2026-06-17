package com.smartcloudbrain.triage.service;

import com.smartcloudbrain.triage.client.AiServiceClient;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class AiGatewayService {

  private final AiServiceClient aiServiceClient;

  public AiGatewayService(AiServiceClient aiServiceClient) {
    this.aiServiceClient = aiServiceClient;
  }

  public TriageResponse triage(TriageRequest request) {
    return call(() -> aiServiceClient.triage(request));
  }

  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    return call(() -> aiServiceClient.generateMedicalRecord(request));
  }

  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    return call(() -> aiServiceClient.checkPrescription(request));
  }

  public PromptResolveResponse resolvePrompt(PromptResolveRequest request) {
    return call(() -> aiServiceClient.resolvePrompt(request));
  }

  private <T> T call(Supplier<Result<T>> supplier) {
    try {
      return unwrap(supplier.get());
    } catch (BusinessException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      throw new BusinessException(600, "AI service request failed: " + ex.getMessage());
    }
  }

  private <T> T unwrap(Result<T> result) {
    if (result == null) {
      throw new BusinessException(600, "AI service returned an empty response");
    }
    if (result.code() != 0) {
      throw new BusinessException(result.code(), result.message());
    }
    return result.data();
  }
}


