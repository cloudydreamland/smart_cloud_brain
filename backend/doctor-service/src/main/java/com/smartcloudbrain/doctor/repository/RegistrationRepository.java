package com.smartcloudbrain.doctor.repository;

import com.smartcloudbrain.doctor.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
  boolean existsBySlotIdAndStatusNot(Long slotId, String status);
}
