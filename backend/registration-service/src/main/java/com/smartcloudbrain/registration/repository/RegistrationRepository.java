package com.smartcloudbrain.registration.repository;

import com.smartcloudbrain.registration.entity.Registration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
  List<Registration> findByPatientId(Long patientId);
  List<Registration> findByOwnerPatientId(Long ownerPatientId);
  List<Registration> findByDoctorId(Long doctorId);
  boolean existsByPatientIdAndSlotIdAndStatusNot(Long patientId, Long slotId, String status);
  boolean existsByOwnerPatientIdAndSubjectTypeAndSubjectIdAndSlotIdAndStatusNot(
      Long ownerPatientId,
      String subjectType,
      Long subjectId,
      Long slotId,
      String status
  );
}


