package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
}
