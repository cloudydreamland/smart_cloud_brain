package com.smartcloudbrain.medicalrecord.controller;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.medicalrecord.dto.medical.MedicalRecordSaveRequest;
import com.smartcloudbrain.medicalrecord.service.AiGatewayService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.medicalrecord.service.MedicalRecordService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/medical-record")
public class MedicalRecordController {

  private final AiGatewayService aiGatewayService;
  private final MedicalRecordService medicalRecordService;

  public MedicalRecordController(AiGatewayService aiGatewayService, MedicalRecordService medicalRecordService) {
    this.aiGatewayService = aiGatewayService;
    this.medicalRecordService = medicalRecordService;
  }

  @PostMapping("/generate")
  public Result<?> generate(@Valid @RequestBody MedicalRecordGenerateRequest request) {
    return Result.success(aiGatewayService.generateMedicalRecord(medicalRecordService.buildGenerateRequest(request)));
  }

  @PostMapping("/save")
  public Result<?> save(@Valid @RequestBody MedicalRecordSaveRequest request) {
    return Result.success(medicalRecordService.save(request));
  }

  @GetMapping("/list")
  public Result<?> list() {
    return Result.success(medicalRecordService.list());
  }

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam("id") Long id) {
    return Result.success(medicalRecordService.detail(id));
  }

  @GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter generateStream(
      @RequestParam("registrationId") Long registrationId,
      @RequestParam("dialogueText") String dialogueText,
      @RequestParam(name = "departmentCode", required = false) String departmentCode
  ) {
    MedicalRecordGenerateRequest aiRequest = medicalRecordService.buildGenerateRequest(
        new MedicalRecordGenerateRequest(registrationId, departmentCode, dialogueText)
    );
    SseEmitter emitter = new SseEmitter(30_000L);
    new Thread(() -> {
      try {
        emitter.send(SseEmitter.event().name("start").data("{\"taskId\":\"mr-" + registrationId + "\"}"));
        emitter.send(SseEmitter.event().name("delta").data("{\"text\":\"AI medical record request submitted.\"}"));
        emitter.send(SseEmitter.event().name("structured").data(aiGatewayService.generateMedicalRecord(
            aiRequest)));
        emitter.send(SseEmitter.event().name("done").data("{\"taskId\":\"mr-" + registrationId + "\"}"));
        emitter.complete();
      } catch (Exception ex) {
        try {
          emitter.send(SseEmitter.event().name("error").data(Map.of("message", ex.getMessage())));
          emitter.complete();
        } catch (IOException ioException) {
          emitter.completeWithError(ioException);
        }
      }
    }, "medical-record-sse-" + registrationId).start();
    return emitter;
  }
}


