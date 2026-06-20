package com.smartcloudbrain.triage.controller;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.InternalRequestGuard;
import com.smartcloudbrain.triage.dto.internal.InternalTriageAssignRequest;
import com.smartcloudbrain.triage.dto.internal.InternalTriageCloseRequest;
import com.smartcloudbrain.triage.service.TriageDeskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/internal/triage-records", "/internal/triage-desk"})
public class InternalTriageDeskController {

  private final TriageDeskService triageDeskService;
  private final InternalRequestGuard internalRequestGuard;

  public InternalTriageDeskController(TriageDeskService triageDeskService, InternalRequestGuard internalRequestGuard) {
    this.triageDeskService = triageDeskService;
    this.internalRequestGuard = internalRequestGuard;
  }

  @GetMapping("/list")
  public Result<?> list() {
    internalRequestGuard.requireServiceRequest();
    return Result.success(triageDeskService.list());
  }

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam("id") Long id) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(triageDeskService.detail(id));
  }

  @PostMapping("/assign")
  public Result<?> assign(@RequestBody InternalTriageAssignRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(triageDeskService.assign(request.triageRecordId(), request.doctorId()));
  }

  @PostMapping("/close")
  public Result<?> close(@RequestBody InternalTriageCloseRequest request) {
    internalRequestGuard.requireServiceRequest();
    return Result.success(triageDeskService.close(request.triageRecordId()));
  }
}
