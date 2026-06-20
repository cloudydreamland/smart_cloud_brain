package com.smartcloudbrain.ai.application;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.ai.provider.mock.MockAiProvider;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
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
  private final MockAiProvider fallbackProvider = new MockAiProvider();

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
        prompt -> aiProvider.triage(request, prompt),
        () -> fallbackProvider.triage(request, null)
    );
  }

  public MedicalRecordGenerateResponse generateMedicalRecord(MedicalRecordGenerateRequest request) {
    return execute(
        "MEDICAL_RECORD",
        request,
        () -> promptTemplateService.resolve("MEDICAL_RECORD", nullToGeneral(request.departmentCode())),
        prompt -> aiProvider.generateMedicalRecord(request, prompt),
        () -> fallbackProvider.generateMedicalRecord(request, null)
    );
  }

  public PrescriptionCheckResponse checkPrescription(PrescriptionCheckRequest request) {
    return execute(
        "PRESCRIPTION_CHECK",
        request,
        () -> promptTemplateService.resolve("PRESCRIPTION_CHECK", "GENERAL"),
        prompt -> aiProvider.checkPrescription(request, prompt),
        () -> fallbackProvider.checkPrescription(request, null)
    );
  }

  public ScheduleSuggestResponse suggestSchedule(ScheduleSuggestRequest request) {
    return execute(
        "SCHEDULE",
        request,
        () -> promptTemplateService.resolve("SCHEDULE", "GENERAL"),
        prompt -> aiProvider.suggestSchedule(request, prompt),
        () -> fallbackProvider.suggestSchedule(request, null)
    );
  }

  private <T> T execute(
      String taskType,
      Object input,
      PromptResolver promptResolver,
      PromptedCallable<T> callable,
      FallbackCallable<T> fallbackCallable
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
      safeRecord(taskType, aiProvider.providerName(), aiProvider.modelName(), requestId, input, null,
          prompt, System.currentTimeMillis() - start, false, rootMessage(ex), ex);
      try {
        T fallback = fallbackCallable.call();
        safeRecord(taskType, fallbackProvider.providerName(), "deterministic-fallback", requestId,
            input, fallback, null, System.currentTimeMillis() - start, true,
            "Fallback after " + aiProvider.providerName() + " failure: " + rootMessage(ex), ex);
        return fallback;
      } catch (RuntimeException fallbackException) {
        fallbackException.addSuppressed(ex);
        throw new BusinessException(600, "AI " + taskType + " and fallback failed: " + rootMessage(fallbackException));
      }
    }
  }

  private void safeRecord(
      String taskType,
      String provider,
      String model,
      String requestId,
      Object input,
      Object output,
      PromptResolveResponse prompt,
      long latencyMs,
      boolean success,
      String errorMessage,
      RuntimeException sourceException
  ) {
    try {
      aiTaskLogService.record(taskType, provider, model, requestId, input, output, prompt,
          latencyMs, success, errorMessage);
    } catch (RuntimeException logException) {
      sourceException.addSuppressed(logException);
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

  @FunctionalInterface
  private interface FallbackCallable<T> {
    T call();
  }
}
