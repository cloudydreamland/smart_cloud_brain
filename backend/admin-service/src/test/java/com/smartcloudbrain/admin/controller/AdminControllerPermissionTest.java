package com.smartcloudbrain.admin.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.service.AdminCatalogService;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminControllerPermissionTest {

  @Mock private AdminCatalogService adminCatalogService;
  @Mock private CurrentUserService currentUserService;

  @Test
  void nonAdminCannotReadAdminCatalog() {
    AdminController controller = new AdminController(adminCatalogService, currentUserService);
    when(currentUserService.require(RoleType.ADMIN)).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

    assertThrows(BusinessException.class, controller::departments);

    verify(adminCatalogService, never()).departments();
  }
}
