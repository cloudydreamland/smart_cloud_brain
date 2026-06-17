package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.MedicalRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
  Optional<MedicalRecord> findFirstByPatientIdAndDoctorIdOrderByIdDesc(Long patientId, Long doctorId);
}
