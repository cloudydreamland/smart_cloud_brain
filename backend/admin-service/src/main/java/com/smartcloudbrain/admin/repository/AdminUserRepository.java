package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.AdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
  Optional<AdminUser> findByUsername(String username);
  long countByStatus(String status);
}
