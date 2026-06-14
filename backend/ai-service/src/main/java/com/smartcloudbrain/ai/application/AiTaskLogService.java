package com.smartcloudbrain.ai.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiTaskLogService {

  private static final Logger log = LoggerFactory.getLogger(AiTaskLogService.class);

  public void recordSuccess(String taskType) {
    log.info("AI task completed: {}", taskType);
  }
}
