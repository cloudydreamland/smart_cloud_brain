package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.KnowledgeEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {
  List<KnowledgeEntry> findByStatus(String status);
}
