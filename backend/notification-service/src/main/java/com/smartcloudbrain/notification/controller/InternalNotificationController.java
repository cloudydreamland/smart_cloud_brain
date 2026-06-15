package com.smartcloudbrain.notification.controller;

import com.smartcloudbrain.common.result.Result;
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

  public InternalNotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @PostMapping
  public Result<?> create(@RequestBody NotificationCreateRequest request) {
    return Result.success(notificationService.create(request));
  }
}

