package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.AiScheduleSuggestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiScheduleSuggestionRepository extends JpaRepository<AiScheduleSuggestion, Long> {
  List<AiScheduleSuggestion> findByStatusOrderByWorkDateAscDoctorIdAsc(String status);
}
