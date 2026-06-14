package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.Doctor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  Optional<Doctor> findByPhone(String phone);
  List<Doctor> findByDepartmentId(Long departmentId);
}
