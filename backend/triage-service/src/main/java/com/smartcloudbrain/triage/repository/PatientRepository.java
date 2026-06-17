package com.smartcloudbrain.triage.repository;

import com.smartcloudbrain.triage.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
