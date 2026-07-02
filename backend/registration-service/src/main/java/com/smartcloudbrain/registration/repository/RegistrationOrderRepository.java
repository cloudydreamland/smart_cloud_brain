package com.smartcloudbrain.registration.repository;

import com.smartcloudbrain.registration.entity.RegistrationOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationOrderRepository extends JpaRepository<RegistrationOrder, Long> {
  Optional<RegistrationOrder> findFirstByRegistrationIdOrderByIdDesc(Long registrationId);
}
