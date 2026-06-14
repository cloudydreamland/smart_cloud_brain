package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.Department;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  Optional<Department> findByCode(String code);
}
