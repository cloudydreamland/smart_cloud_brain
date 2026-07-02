package com.smartcloudbrain.registration.repository;

import com.smartcloudbrain.registration.entity.PatientVisitor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientVisitorRepository extends JpaRepository<PatientVisitor, Long> {
  Optional<PatientVisitor> findByIdAndOwnerPatientId(Long id, Long ownerPatientId);
}
