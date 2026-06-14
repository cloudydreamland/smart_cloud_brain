package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.TriageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
}
