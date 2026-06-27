package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.PatientSiteConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientSiteConfigRepository extends JpaRepository<PatientSiteConfig, Long> {
  List<PatientSiteConfig> findByConfigKeyOrderByVersionDesc(String configKey);
  Page<PatientSiteConfig> findByConfigKeyOrderByVersionDesc(String configKey, Pageable pageable);
  List<PatientSiteConfig> findByConfigKeyAndStatusOrderByVersionDesc(String configKey, String status);
  List<PatientSiteConfig> findByConfigKeyAndStatusOrderByVersionAsc(String configKey, String status, Pageable pageable);
  Optional<PatientSiteConfig> findFirstByConfigKeyAndStatusOrderByVersionDesc(String configKey, String status);
  long countByConfigKey(String configKey);
  List<PatientSiteConfig> findByStatus(String status);
}
