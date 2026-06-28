package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import org.springframework.stereotype.Service;

@Service
public class AdminPermissionGuard {

  private final CurrentUserService currentUserService;
  private final AdminOperationsService adminOperationsService;

  public AdminPermissionGuard(CurrentUserService currentUserService, AdminOperationsService adminOperationsService) {
    this.currentUserService = currentUserService;
    this.adminOperationsService = adminOperationsService;
  }

  public AuthenticatedUser requirePermission(String permissionKey) {
    AuthenticatedUser user = currentUserService.require(RoleType.ADMIN);
    if (!adminOperationsService.hasPermission(user.role(), permissionKey)) {
      throw new BusinessException(403, "Permission denied: " + permissionKey);
    }
    return user;
  }
}
