package com.smartcloudbrain.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.auth.service.AuthService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerCurrentUserTest {

  @Mock private AuthService authService;
  @Mock private CurrentUserService currentUserService;

  @Test
  void currentUserReturnsGatewayInjectedUserContext() {
    when(currentUserService.get()).thenReturn(new AuthenticatedUser(7L, RoleType.DOCTOR, "doctor1"));
    AuthController controller = new AuthController(authService, currentUserService);

    Result<?> result = controller.currentUser();

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) result.data();
    assertEquals(0, result.code());
    assertEquals(7L, data.get("userId"));
    assertEquals("DOCTOR", data.get("role"));
    assertEquals("doctor1", data.get("name"));
  }
}
