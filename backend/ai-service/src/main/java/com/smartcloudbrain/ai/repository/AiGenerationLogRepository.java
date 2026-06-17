package com.smartcloudbrain.ai.repository;

import com.smartcloudbrain.ai.entity.AiGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {
}
