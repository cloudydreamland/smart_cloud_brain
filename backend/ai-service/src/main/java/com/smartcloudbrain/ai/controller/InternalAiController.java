package com.smartcloudbrain.ai.controller;

import com.smartcloudbrain.ai.application.AiOrchestrationService;
import com.smartcloudbrain.aiapi.constant.AiInternalApi;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PromptResolveRequest;
import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.ai.service.PromptTemplateService;
import com.smartcloudbrain.common.result.Result;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(AiInternalApi.BASE)
public class InternalAiController {

  private final AiOrchestrationService aiOrchestrationService;
  private final PromptTemplateService promptTemplateService;

  public InternalAiController(AiOrchestrationService aiOrchestrationService, PromptTemplateService promptTemplateService) {
    this.aiOrchestrationService = aiOrchestrationService;
    this.promptTemplateService = promptTemplateService;
  }

  @PostMapping("/triage")
  public Result<?> triage(@Valid @RequestBody TriageRequest request) {
    return Result.success(aiOrchestrationService.triage(request));
  }

  @PostMapping("/medical-record/generate")
  public Result<?> generateMedicalRecord(@Valid @RequestBody MedicalRecordGenerateRequest request) {
    return Result.success(aiOrchestrationService.generateMedicalRecord(request));
  }

  @GetMapping(value = "/medical-record/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamMedicalRecord(
      @RequestParam Long registrationId,
      @RequestParam String dialogueText,
      @RequestParam(required = false) String departmentCode
  ) {
    SseEmitter emitter = new SseEmitter(30_000L);
    new Thread(() -> {
      try {
        emitter.send(SseEmitter.event().name("start").data("{\"taskId\":\"mr-" + registrationId + "\"}"));
        emitter.send(SseEmitter.event().name("delta").data("{\"text\":\"AI medical record request submitted.\"}"));
        emitter.send(SseEmitter.event().name("structured").data(aiOrchestrationService.generateMedicalRecord(new MedicalRecordGenerateRequest(registrationId, departmentCode, dialogueText))));
        emitter.send(SseEmitter.event().name("done").data("{\"taskId\":\"mr-" + registrationId + "\"}"));
        emitter.complete();
      } catch (Exception ex) {
        try {
          emitter.send(SseEmitter.event().name("error").data("{\"message\":\"" + ex.getMessage() + "\"}"));
          emitter.complete();
        } catch (IOException ioException) {
          emitter.completeWithError(ioException);
        }
      }
    }, "ai-medical-record-sse-" + registrationId).start();
    return emitter;
  }

  @PostMapping("/prescription/check")
  public Result<?> checkPrescription(@Valid @RequestBody PrescriptionCheckRequest request) {
    return Result.success(aiOrchestrationService.checkPrescription(request));
  }

  @PostMapping("/prompt-template/resolve")
  public Result<?> resolvePrompt(@Valid @RequestBody PromptResolveRequest request) {
    return Result.success(promptTemplateService.resolve(request.taskType(), request.departmentCode()));
  }
}
