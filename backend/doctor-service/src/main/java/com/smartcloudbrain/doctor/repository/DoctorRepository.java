package com.smartcloudbrain.doctor.repository;

import com.smartcloudbrain.doctor.entity.Doctor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  Optional<Doctor> findByPhone(String phone);
  List<Doctor> findByDepartmentId(Long departmentId);
}


