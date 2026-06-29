package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.PatientRecommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRecommendationRepository extends JpaRepository<PatientRecommendation, Long> {
  List<PatientRecommendation> findByRecommendTypeAndDeletedFalseOrderBySortAscUpdatedAtDesc(String recommendType);
  List<PatientRecommendation> findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc(String recommendType, String status);
}
