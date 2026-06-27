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
  public Result<?> list(
      @RequestParam(name = "readStatus", required = false) String readStatus,
      @RequestParam(name = "handleStatus", required = false) String handleStatus,
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "riskLevel", required = false) String riskLevel,
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "sort", required = false) String sort
  ) {
    return Result.success(notificationService.list(readStatus, handleStatus, type, riskLevel, q, sort));
  }

  @PostMapping("/read")
  public Result<?> read(@RequestBody Map<String, Long> request) {
    return Result.success(notificationService.markRead(request.get("notificationId")));
  }

  @PostMapping("/handle")
  public Result<?> handle(@RequestBody Map<String, Object> request) {
    Long notificationId = Long.valueOf(String.valueOf(request.get("notificationId")));
    String handleStatus = String.valueOf(request.get("handleStatus"));
    return Result.success(notificationService.handle(notificationId, handleStatus));
  }
}

