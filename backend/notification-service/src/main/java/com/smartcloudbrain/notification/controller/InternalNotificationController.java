package com.smartcloudbrain.notification.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.notification.dto.NotificationCreateRequest;
import com.smartcloudbrain.notification.service.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

  private final NotificationService notificationService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalNotificationController(NotificationService notificationService, InternalRequestGuard internalRequestGuard) {
    this.notificationService = notificationService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @PostMapping
  public Result<?> create(@RequestBody NotificationCreateRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(notificationService.create(request));
  }
}

