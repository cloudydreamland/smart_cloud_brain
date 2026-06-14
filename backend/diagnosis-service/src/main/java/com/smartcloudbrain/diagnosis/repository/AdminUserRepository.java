package com.smartcloudbrain.diagnosis.repository;

import com.smartcloudbrain.diagnosis.entity.AdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
  Optional<AdminUser> findByUsername(String username);
}
