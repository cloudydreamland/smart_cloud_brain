package com.smartcloudbrain.ai.repository;

import com.smartcloudbrain.ai.entity.AiGenerationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

  List<AiGenerationLog> findTop20ByOrderByCreatedAtDesc();
}
