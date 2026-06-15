package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.TriageRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
  List<TriageRecord> findAllByOrderByIdDesc();
}
