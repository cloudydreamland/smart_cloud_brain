package com.smartcloudbrain.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.ai.entity.AiGenerationLog;
import com.smartcloudbrain.ai.repository.AiGenerationLogRepository;
import com.smartcloudbrain.aiapi.dto.PromptResolveResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
      repository.saveAndFlush(entry);
    } catch (RuntimeException ex) {
      log.error("Failed to persist AI generation log for task {} request {}", taskType, requestId, ex);
    }
  }

  public List<Map<String, Object>> recentLogs() {
    return repository.findTop20ByOrderByCreatedAtDesc().stream()
        .map(entry -> {
          Map<String, Object> view = new LinkedHashMap<>();
          view.put("taskType", nullToEmpty(entry.getTaskType()));
          view.put("provider", nullToEmpty(entry.getProvider()));
          view.put("model", nullToEmpty(entry.getModel()));
          view.put("requestId", nullToEmpty(entry.getRequestId()));
          view.put("status", nullToEmpty(entry.getStatus()));
          view.put("success", Boolean.TRUE.equals(entry.getSuccess()));
          view.put("latencyMs", entry.getLatencyMs() == null ? 0L : entry.getLatencyMs());
          view.put("promptTemplateId", entry.getPromptTemplateId() == null ? 0L : entry.getPromptTemplateId());
          view.put("errorMessage", nullToEmpty(entry.getErrorMessage()));
          view.put("createdAt", entry.getCreatedAt() == null ? "" : entry.getCreatedAt().toString());
          return view;
        })
        .toList();
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

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
