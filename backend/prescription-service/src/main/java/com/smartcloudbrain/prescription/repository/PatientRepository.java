package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
