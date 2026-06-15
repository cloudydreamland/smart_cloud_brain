package com.smartcloudbrain.registration.repository;

import com.smartcloudbrain.registration.entity.Patient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
  Optional<Patient> findByPhone(String phone);
}


