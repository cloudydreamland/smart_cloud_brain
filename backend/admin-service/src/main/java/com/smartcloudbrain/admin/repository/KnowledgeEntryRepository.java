package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.KnowledgeEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long> {
  List<KnowledgeEntry> findByStatus(String status);
}


