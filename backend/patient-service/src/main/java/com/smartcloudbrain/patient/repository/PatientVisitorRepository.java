package com.smartcloudbrain.patient.repository;

import com.smartcloudbrain.patient.entity.PatientVisitor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientVisitorRepository extends JpaRepository<PatientVisitor, Long> {
  List<PatientVisitor> findByOwnerPatientIdOrderByIdAsc(Long ownerPatientId);

  Optional<PatientVisitor> findByIdAndOwnerPatientId(Long id, Long ownerPatientId);
}
