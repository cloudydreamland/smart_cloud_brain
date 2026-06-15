package com.smartcloudbrain.prescription.repository;

import com.smartcloudbrain.prescription.entity.PrescriptionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
  List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);
}


