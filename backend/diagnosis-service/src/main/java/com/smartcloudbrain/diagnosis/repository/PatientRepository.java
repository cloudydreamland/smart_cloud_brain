package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.Patient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
  Optional<Patient> findByPhone(String phone);
}
