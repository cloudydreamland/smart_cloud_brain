package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.Prescription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
  List<Prescription> findByPatientId(Long patientId);
  List<Prescription> findByDoctorId(Long doctorId);
}
