package com.smartcloudbrain.patient.repository;

import com.smartcloudbrain.patient.entity.Doctor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  List<Doctor> findByDepartmentId(Long departmentId);
}
