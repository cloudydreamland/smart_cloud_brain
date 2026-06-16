package com.smartcloudbrain.triage.controller;

import com.smartcloudbrain.common.result.Result;
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

  public InternalTriageDeskController(TriageDeskService triageDeskService) {
    this.triageDeskService = triageDeskService;
  }

  @GetMapping("/list")
  public Result<?> list() {
    return Result.success(triageDeskService.list());
  }

  @GetMapping("/detail")
  public Result<?> detail(@RequestParam Long id) {
    return Result.success(triageDeskService.detail(id));
  }

  @PostMapping("/assign")
  public Result<?> assign(@RequestBody InternalTriageAssignRequest request) {
    return Result.success(triageDeskService.assign(request.triageRecordId(), request.doctorId()));
  }

  @PostMapping("/close")
  public Result<?> close(@RequestBody InternalTriageCloseRequest request) {
    return Result.success(triageDeskService.close(request.triageRecordId()));
  }
}
