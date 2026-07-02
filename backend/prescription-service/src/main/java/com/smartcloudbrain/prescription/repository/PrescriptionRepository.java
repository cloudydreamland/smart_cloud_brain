package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.Prescription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
  List<Prescription> findByPatientId(Long patientId);
  List<Prescription> findByOwnerPatientId(Long ownerPatientId);
  List<Prescription> findByDoctorId(Long doctorId);
}


