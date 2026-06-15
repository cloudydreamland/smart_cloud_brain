package com.smartcloudbrain.medicalrecord.repository;

import com.smartcloudbrain.medicalrecord.entity.MedicalRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
  List<MedicalRecord> findByPatientId(Long patientId);
  List<MedicalRecord> findByDoctorId(Long doctorId);
  Optional<MedicalRecord> findByRegistrationId(Long registrationId);
}


