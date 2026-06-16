package com.smartcloudbrain.triage.repository;

import com.smartcloudbrain.triage.entity.TriageRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {
  List<TriageRecord> findByPatientId(Long patientId);
  List<TriageRecord> findAllByOrderByIdDesc();
}


