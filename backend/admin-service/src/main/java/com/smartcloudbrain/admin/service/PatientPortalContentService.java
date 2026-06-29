package com.smartcloudbrain.admin.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientPortalContentService {

  private final PatientSiteConfigService configService;
  private final PatientNoticeService noticeService;
  private final PatientRecommendationService recommendationService;

  public PatientPortalContentService(
      PatientSiteConfigService configService,
      PatientNoticeService noticeService,
      PatientRecommendationService recommendationService
  ) {
    this.configService = configService;
    this.noticeService = noticeService;
    this.recommendationService = recommendationService;
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "patientSite", key = "'home'")
  public Map<String, Object> home() {
    Map<String, Object> config = configService.publicConfig();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("nav", config.get("nav"));
    view.put("home", config.get("home"));
    view.put("hospitalInfo", config.get("hospitalInfo"));
    view.put("notices", noticeService.publicList());
    view.put("hotDepartments", recommendationService.publicList("DEPARTMENT"));
    view.put("recommendedDoctors", recommendationService.publicList("DOCTOR"));
    view.put("footer", config.get("footer"));
    return view;
  }
}
