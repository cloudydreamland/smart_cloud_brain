package com.smartcloudbrain.triage.repository;

import com.smartcloudbrain.triage.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}
