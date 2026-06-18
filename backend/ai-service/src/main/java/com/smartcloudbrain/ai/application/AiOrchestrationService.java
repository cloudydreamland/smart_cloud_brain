package com.smartcloudbrain.ai.application;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiOrchestrationService {

  private final AiProvider aiProvider;
  private final AiTaskLogService aiTaskLogService;
  private final PromptTemplateService promptTemplateService;

  public AiOrchestrationService(
      AiProvider aiProvider,
      AiTaskLogService aiTaskLogService,
      PromptTemplateService promptTemplateService
  ) {
    this.aiProvider = aiProvider;
    this.aiTaskLogService = aiTaskLogService;
    this.promptTemplateService = promptTemplateService;
  }

  public TriageResponse triage(TriageRequest request) {
    return execute(
        "TRIAGE",
        request,
        () -> promptTemplateService.resolve("TRIAGE", "GENERAL"),
        prompt -> aiProvider.triage(request, prompt)
    );
  }

  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    return execute(
        "MEDICAL_RECORD",
        request,
        () -> promptTemplateService.resolve("MEDICAL_RECORD", nullToGeneral(request.departmentCode())),
        prompt -> aiProvider.generateMedicalRecord(request, prompt)
    );
  }

  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    return execute(
        "PRESCRIPTION_CHECK",
        request,
        () -> promptTemplateService.resolve("PRESCRIPTION_CHECK", "GENERAL"),
        prompt -> aiProvider.checkPrescription(request, prompt)
    );
  }

  private <T> T execute(
      String taskType,
      Object input,
      PromptResolver promptResolver,
      PromptedCallable<T> callable
  ) {
    String requestId = UUID.randomUUID().toString();
    long start = System.currentTimeMillis();
    PromptResolveResponse prompt = null;
    try {
      prompt = promptResolver.resolve();
      T result = callable.call(prompt);
      aiTaskLogService.record(
          taskType,
          aiProvider.providerName(),
          aiProvider.modelName(),
          requestId,
          input,
          result,
          prompt,
          System.currentTimeMillis() - start,
          true,
          ""
      );
      return result;
    } catch (RuntimeException ex) {
      try {
        aiTaskLogService.record(
            taskType,
            aiProvider.providerName(),
            aiProvider.modelName(),
            requestId,
            input,
            null,
            prompt,
            System.currentTimeMillis() - start,
            false,
            rootMessage(ex)
        );
      } catch (RuntimeException logException) {
        ex.addSuppressed(logException);
      }
      if (ex instanceof BusinessException businessException) {
        throw businessException;
      }
      throw new BusinessException(600, "AI " + taskType + " failed: " + rootMessage(ex));
    }
  }

  private String nullToGeneral(String value) {
    return value == null || value.isBlank() ? "GENERAL" : value;
  }

  private String rootMessage(Throwable ex) {
    Throwable current = ex;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() == null ? ex.getClass().getSimpleName() : current.getMessage();
  }

  @FunctionalInterface
  private interface PromptedCallable<T> {
    T call(PromptResolveResponse prompt);
  }

  @FunctionalInterface
  private interface PromptResolver {
    PromptResolveResponse resolve();
  }
}
