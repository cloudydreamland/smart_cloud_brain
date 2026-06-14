package com.smartcloudbrain.diagnosis.controller;

import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.diagnosis.service.AiGatewayService;
import com.smartcloudbrain.common.result.Result;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
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

  public MedicalRecordController(AiGatewayService aiGatewayService) {
    this.aiGatewayService = aiGatewayService;
  }

  @PostMapping("/generate")
  public Result<?> generate(@Valid @RequestBody MedicalRecordGenerateRequest request) {
    return Result.success(aiGatewayService.generateMedicalRecord(request));
  }

  @GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter generateStream(@RequestParam Long registrationId) {
    SseEmitter emitter = new SseEmitter(30_000L);
    new Thread(() -> {
      try {
        emitter.send(SseEmitter.event().name("start").data("{\"taskId\":\"mr-" + registrationId + "\"}"));
        for (String text : List.of("Connecting to AI medical record service.", "Organizing consultation text.", "Structured medical record draft generated.")) {
          emitter.send(SseEmitter.event().name("delta").data("{\"text\":\"" + text + "\"}"));
          Thread.sleep(300L);
        }
        emitter.send(SseEmitter.event().name("structured").data(aiGatewayService.generateMedicalRecord(
            new MedicalRecordGenerateRequest(registrationId, "CARDIOLOGY", "stream request"))));
        emitter.send(SseEmitter.event().name("done").data("{\"taskId\":\"mr-" + registrationId + "\"}"));
        emitter.complete();
      } catch (IOException | InterruptedException ex) {
        emitter.completeWithError(ex);
        Thread.currentThread().interrupt();
      }
    }, "medical-record-sse-" + registrationId).start();
    return emitter;
  }
}
