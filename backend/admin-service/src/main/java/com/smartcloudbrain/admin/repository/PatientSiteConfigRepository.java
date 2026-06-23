package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.PatientSiteConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientSiteConfigRepository extends JpaRepository<PatientSiteConfig, Long> {
  List<PatientSiteConfig> findByConfigKeyOrderByVersionDesc(String configKey);
  List<PatientSiteConfig> findByConfigKeyAndStatusOrderByVersionDesc(String configKey, String status);
  Optional<PatientSiteConfig> findFirstByConfigKeyAndStatusOrderByVersionDesc(String configKey, String status);
  List<PatientSiteConfig> findByStatus(String status);
}
