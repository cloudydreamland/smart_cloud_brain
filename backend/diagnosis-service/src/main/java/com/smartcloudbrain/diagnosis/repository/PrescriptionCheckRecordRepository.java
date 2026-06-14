package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.PrescriptionCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionCheckRecordRepository extends JpaRepository<PrescriptionCheckRecord, Long> {
}
