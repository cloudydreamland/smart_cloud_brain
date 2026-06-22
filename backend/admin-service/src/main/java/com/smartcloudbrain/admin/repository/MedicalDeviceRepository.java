package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.MedicalDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalDeviceRepository extends JpaRepository<MedicalDevice, Long> {
  Optional<MedicalDevice> findByDeviceCode(String deviceCode);
}
