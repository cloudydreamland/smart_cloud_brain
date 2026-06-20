package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.Drug;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugRepository extends JpaRepository<Drug, Long> {

  List<Drug> findByStatusIgnoreCase(String status);
}
