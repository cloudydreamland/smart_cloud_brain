package com.smartcloudbrain.medicalrecord.repository;

import com.smartcloudbrain.medicalrecord.entity.Registration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
  List<Registration> findByPatientId(Long patientId);
  List<Registration> findByDoctorId(Long doctorId);
}


