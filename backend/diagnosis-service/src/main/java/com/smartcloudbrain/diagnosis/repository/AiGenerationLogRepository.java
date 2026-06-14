package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.AiGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {
}
