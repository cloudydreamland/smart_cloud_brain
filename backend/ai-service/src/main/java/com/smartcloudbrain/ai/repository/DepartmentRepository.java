package com.smartcloudbrain.ai.repository;

import com.smartcloudbrain.ai.entity.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  List<Department> findAllByOrderByIdAsc();
}
