package com.smartcloudbrain.admin.controller;

import com.smartcloudbrain.admin.service.AdminCatalogService;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogSearchController {

  private final AdminCatalogService adminCatalogService;
  private final CurrentUserService currentUserService;

  public CatalogSearchController(AdminCatalogService adminCatalogService, CurrentUserService currentUserService) {
    this.adminCatalogService = adminCatalogService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/api/search/knowledge")
  public Result<?> searchKnowledge(
      @RequestParam(name = "q", defaultValue = "") String q,
      @RequestParam(name = "departmentCode", defaultValue = "") String departmentCode
  ) {
    currentUserService.get();
    return Result.success(adminCatalogService.searchKnowledge(q, departmentCode));
  }

  @GetMapping("/api/search/drugs")
  public Result<?> searchDrugs(@RequestParam(name = "q", defaultValue = "") String q) {
    currentUserService.get();
    return Result.success(adminCatalogService.searchDrugs(q));
  }

  @GetMapping("/api/admin/search/prompts")
  public Result<?> searchPrompts(@RequestParam(name = "q", defaultValue = "") String q) {
    currentUserService.require(RoleType.ADMIN);
    return Result.success(adminCatalogService.searchPrompts(q));
  }

}
