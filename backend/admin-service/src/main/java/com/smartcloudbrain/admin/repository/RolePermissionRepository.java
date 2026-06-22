package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.RolePermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
  List<RolePermission> findByRoleOrderByPermissionKeyAsc(String role);
  List<RolePermission> findByRoleAndEnabledTrueOrderByPermissionKeyAsc(String role);
  Optional<RolePermission> findByRoleAndPermissionKey(String role, String permissionKey);
  long countByRole(String role);
}
