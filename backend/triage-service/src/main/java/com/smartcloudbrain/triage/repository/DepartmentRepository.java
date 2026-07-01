package com.smartcloudbrain.triage.repository;

import com.smartcloudbrain.triage.entity.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  Optional<Department> findByCodeIgnoreCase(String code);
  List<Department> findAllByOrderByIdAsc();
}
