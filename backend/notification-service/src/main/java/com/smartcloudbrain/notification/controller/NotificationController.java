package com.smartcloudbrain.notification.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.notification.service.NotificationService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/list")
  public Result<?> list(@RequestParam(name = "readStatus", required = false) String readStatus) {
    return Result.success(notificationService.list(readStatus));
  }

  @PostMapping("/read")
  public Result<?> read(@RequestBody Map<String, Long> request) {
    return Result.success(notificationService.markRead(request.get("notificationId")));
  }
}


