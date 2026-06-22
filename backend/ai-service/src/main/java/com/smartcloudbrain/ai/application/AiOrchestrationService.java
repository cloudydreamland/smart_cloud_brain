package com.smartcloudbrain.ai.application;

import com.smartcloudbrain.ai.provider.AiProvider;
import com.smartcloudbrain.ai.provider.mock.MockAiProvider;
import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateResponse;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import com.smartcloudbrain.aiapi.dto.PromptTestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
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

  public Object testPrompt(PromptTestRequest request) {
    String taskType = normalizeTaskType(request.taskType());
    PromptResolveResponse prompt = new PromptResolveResponse(
        null,
        taskType,
        nullToGeneral(request.departmentCode()),
        blankToDefault(request.templateName(), "PROMPT_TEST"),
        request.templateContent(),
        request.outputSchema(),
        "test"
    );
    return switch (taskType) {
      case "TRIAGE" -> execute(
          "PROMPT_TEST_TRIAGE",
          request,
          () -> prompt,
          resolvedPrompt -> aiProvider.triage(sampleTriageRequest(request.sampleInput()), resolvedPrompt),
          () -> fallbackProvider.triage(sampleTriageRequest(request.sampleInput()), null)
      );
      case "MEDICAL_RECORD" -> execute(
          "PROMPT_TEST_MEDICAL_RECORD",
          request,
          () -> prompt,
          resolvedPrompt -> aiProvider.generateMedicalRecord(sampleMedicalRecordRequest(request), resolvedPrompt),
          () -> fallbackProvider.generateMedicalRecord(sampleMedicalRecordRequest(request), null)
      );
      case "PRESCRIPTION_CHECK" -> execute(
          "PROMPT_TEST_PRESCRIPTION_CHECK",
          request,
          () -> prompt,
          resolvedPrompt -> aiProvider.checkPrescription(samplePrescriptionCheckRequest(request.sampleInput()), resolvedPrompt),
          () -> fallbackProvider.checkPrescription(samplePrescriptionCheckRequest(request.sampleInput()), null)
      );
      default -> throw new BusinessException(400, "Unsupported AI task type: " + request.taskType());
    };
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
      T result = withRuntime(callable.call(prompt), aiProvider.providerName(), aiProvider.modelName());
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
        T fallback = withRuntime(fallbackCallable.call(), fallbackProvider.providerName(), fallbackProvider.modelName());
        safeRecord(taskType, fallbackProvider.providerName(), fallbackProvider.modelName(), requestId,
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

  @SuppressWarnings("unchecked")
  private <T> T withRuntime(T result, String provider, String model) {
    if (result instanceof TriageResponse response) {
      return (T) response.withRuntime(provider, model);
    }
    if (result instanceof MedicalRecordGenerateResponse response) {
      return (T) response.withRuntime(provider, model);
    }
    if (result instanceof PrescriptionCheckResponse response) {
      return (T) response.withRuntime(provider, model);
    }
    if (result instanceof ScheduleSuggestResponse response) {
      return (T) response.withRuntime(provider, model);
    }
    return result;
  }

  private String nullToGeneral(String value) {
    return value == null || value.isBlank() ? "GENERAL" : value;
  }

  private String normalizeTaskType(String taskType) {
    return taskType == null ? "" : taskType.trim().toUpperCase();
  }

  private String blankToDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private TriageRequest sampleTriageRequest(String sampleInput) {
    return new TriageRequest(
        1L,
        blankToDefault(sampleInput, "胸痛、气短两天，活动后加重"),
        "胸痛，气短，活动后加重",
        45,
        "MALE",
        "青霉素过敏",
        "高血压病史三年"
    );
  }

  private MedicalRecordGenerateRequest sampleMedicalRecordRequest(PromptTestRequest request) {
    return new MedicalRecordGenerateRequest(
        1L,
        nullToGeneral(request.departmentCode()),
        blankToDefault(request.sampleInput(), "患者胸痛、气短两天，活动后加重，休息后稍缓解。"),
        1L,
        "测试患者",
        45,
        "MALE",
        "青霉素过敏",
        "高血压病史三年",
        2L,
        "测试医生",
        LocalDateTime.now().toString()
    );
  }

  private PrescriptionCheckRequest samplePrescriptionCheckRequest(String sampleInput) {
    return new PrescriptionCheckRequest(
        1L,
        2L,
        1L,
        blankToDefault(sampleInput, "胸痛待查，高血压"),
        45,
        "MALE",
        "青霉素过敏",
        "高血压病史三年",
        List.of(new DrugItem("aspirin", "100mg", "once daily", "oral", 7, "take after meals"))
    );
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
