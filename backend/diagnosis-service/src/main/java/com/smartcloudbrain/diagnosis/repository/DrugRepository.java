package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.Drug;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugRepository extends JpaRepository<Drug, Long> {
}
