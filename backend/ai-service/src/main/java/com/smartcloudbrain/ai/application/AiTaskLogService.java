package com.smartcloudbrain.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.entity.AiGenerationLog;
import com.smartcloudbrain.ai.repository.AiGenerationLogRepository;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTaskLogService {

  private static final Logger log = LoggerFactory.getLogger(AiTaskLogService.class);
  private static final int SUMMARY_LIMIT = 500;

  private final AiGenerationLogRepository repository;
  private final ObjectMapper objectMapper;

  public AiTaskLogService(AiGenerationLogRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      String taskType,
      String provider,
      String model,
      String requestId,
      Object input,
      Object output,
      PromptResolveResponse prompt,
      long latencyMs,
      boolean success,
      String errorMessage
  ) {
    try {
      AiGenerationLog entry = new AiGenerationLog();
      entry.setTaskType(taskType);
      entry.setProvider(provider);
      entry.setModel(model);
      entry.setRequestId(requestId);
      entry.setInputSummary(summary(input));
      entry.setOutputSummary(success ? summary(output) : "");
      entry.setSuccess(success);
      entry.setStatus(success ? "SUCCESS" : "FAILED");
      entry.setErrorMessage(summary(errorMessage));
      entry.setLatencyMs(latencyMs);
      entry.setPromptTemplateId(prompt == null ? null : prompt.promptTemplateId());
      entry.setCreatedAt(LocalDateTime.now());
      repository.save(entry);
    } catch (RuntimeException ex) {
      log.error("Failed to persist AI generation log for task {} request {}", taskType, requestId, ex);
      throw ex;
    }
  }

  private String summary(Object value) {
    if (value == null) {
      return "";
    }
    String text;
    if (value instanceof String stringValue) {
      text = stringValue;
    } else {
      try {
        text = objectMapper.writeValueAsString(value);
      } catch (Exception ex) {
        text = String.valueOf(value);
      }
    }
    text = text.replaceAll("\\s+", " ").trim();
    if (text.length() <= SUMMARY_LIMIT) {
      return text;
    }
    return text.substring(0, SUMMARY_LIMIT);
  }
}
