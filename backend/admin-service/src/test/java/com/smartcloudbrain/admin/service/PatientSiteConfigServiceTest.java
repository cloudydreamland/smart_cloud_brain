package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigHistoryResponse;
import com.smartcloudbrain.admin.entity.PatientSiteConfig;
import com.smartcloudbrain.admin.repository.PatientSiteConfigRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PatientSiteConfigServiceTest {

  @Mock private PatientSiteConfigRepository repository;
  private PatientSiteConfigService service;

  @BeforeEach
  void setUp() {
    service = new PatientSiteConfigService(repository, new ObjectMapper(), "smart-cloud-brain-test-preview-secret");
  }

  @Test
  void savesValidDraftConfig() {
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "DRAFT"))
        .thenReturn(Optional.empty());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_nav")).thenReturn(List.of());
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> {
      PatientSiteConfig value = invocation.getArgument(0);
      value.setId(1L);
      return value;
    });

    Map<String, Object> saved = service.saveDraft(new PatientSiteConfigSaveRequest(
        "patient_nav",
        "{\"brand\":{\"name\":\"智慧云脑\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}",
        "nav"
    ), 9L);

    assertEquals("patient_nav", saved.get("configKey"));
    assertEquals("DRAFT", saved.get("status"));
    assertEquals(1, saved.get("version"));
    assertEquals(9L, saved.get("createdBy"));
    assertEquals(9L, saved.get("updatedBy"));
  }

  @Test
  void savesDraftUpdatesExistingDraftWithoutChangingVersion() {
    PatientSiteConfig draft = config("patient_nav", "DRAFT", 7,
        "{\"brand\":{\"name\":\"Old\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}");
    draft.setId(42L);
    draft.setCreatedBy(5L);
    draft.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "DRAFT"))
        .thenReturn(Optional.of(draft));
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> saved = service.saveDraft(new PatientSiteConfigSaveRequest(
        "patient_nav",
        "{\"brand\":{\"name\":\"New\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}",
        "updated"
    ), 9L);

    assertEquals(42L, saved.get("id"));
    assertEquals("patient_nav", saved.get("configKey"));
    assertEquals("DRAFT", saved.get("status"));
    assertEquals(7, saved.get("version"));
    assertEquals(5L, saved.get("createdBy"));
    assertEquals(9L, saved.get("updatedBy"));
    assertEquals("2026-01-02T03:04:05", saved.get("createdAt"));
    assertEquals("updated", saved.get("remark"));
    assertEquals("{\"brand\":{\"name\":\"New\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}", saved.get("configJson"));
    verify(repository, org.mockito.Mockito.never()).findByConfigKeyOrderByVersionDesc("patient_nav");
  }

  @Test
  void trimsOldArchivedVersionsWhenHistoryExceedsRetentionLimit() {
    PatientSiteConfig draft = config("patient_nav", "DRAFT", 51,
        "{\"brand\":{\"name\":\"Old\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}");
    PatientSiteConfig archived1 = config("patient_nav", "ARCHIVED", 1, "{}");
    PatientSiteConfig archived2 = config("patient_nav", "ARCHIVED", 2, "{}");
    PatientSiteConfig archived3 = config("patient_nav", "ARCHIVED", 3, "{}");
    List<PatientSiteConfig> expired = List.of(archived1, archived2, archived3);
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "DRAFT"))
        .thenReturn(Optional.of(draft));
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.countByConfigKey("patient_nav")).thenReturn(53L);
    when(repository.findByConfigKeyAndStatusOrderByVersionAsc(eq("patient_nav"), eq("ARCHIVED"), any(Pageable.class)))
        .thenReturn(expired);

    service.saveDraft(new PatientSiteConfigSaveRequest(
        "patient_nav",
        "{\"brand\":{\"name\":\"New\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}",
        "updated"
    ), 9L);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findByConfigKeyAndStatusOrderByVersionAsc(eq("patient_nav"), eq("ARCHIVED"), pageableCaptor.capture());
    assertEquals(3, pageableCaptor.getValue().getPageSize());
    verify(repository).deleteAll(expired);
  }

  @Test
  void rejectsInvalidJsonAndUnknownKeyOnSave() {
    assertThrows(BusinessException.class, () -> service.saveDraft(
        new PatientSiteConfigSaveRequest("patient_nav", "{bad", null), 1L));
    assertThrows(BusinessException.class, () -> service.saveDraft(
        new PatientSiteConfigSaveRequest("unknown", "{}", null), 1L));
  }

  @Test
  void historyReturnsPagedRecordsAndMetadata() {
    PatientSiteConfig published = config("patient_pages", "PUBLISHED", 5, "{\"pages\":[]}");
    PatientSiteConfig archived = config("patient_pages", "ARCHIVED", 4, "{\"pages\":[]}");
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "PUBLISHED"))
        .thenReturn(List.of(published));
    when(repository.findByConfigKeyOrderByVersionDesc(eq("patient_pages"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(published, archived), PageRequest.of(1, 2), 5));

    PatientSiteConfigHistoryResponse result = service.history("patient_pages", 2, 2);

    assertEquals(2, result.page());
    assertEquals(2, result.pageSize());
    assertEquals(5L, result.total());
    assertEquals(3, result.totalPages());
    assertEquals(2, result.items().size());
  }

  @Test
  void rejectsPublishWithoutRemark() {
    assertThrows(BusinessException.class, () -> service.savePublished(
        new PatientSiteConfigSaveRequest("patient_nav", "{\"brand\":{\"name\":\"Smart\",\"homeRoute\":\"patient-home\"},\"menus\":[],\"userLinks\":[]}", " "), 1L));
    assertThrows(BusinessException.class, () -> service.publish("patient_nav", "", 1L));
  }

  @Test
  void rejectsPublishWithUnknownRouteName() {
    PatientSiteConfig draft = config("patient_static_pages", "DRAFT", 1,
        "{\"pages\":[{\"routeName\":\"bad-route\",\"title\":\"Bad\",\"points\":[]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_static_pages", "publish", 1L));
  }

  @Test
  void allowsExistingPublicResearchRoute() {
    PatientSiteConfig draft = config("patient_static_pages", "DRAFT", 1,
        "{\"pages\":[{\"routeName\":\"public-research\",\"title\":\"Research\",\"points\":[]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "DRAFT"))
        .thenReturn(Optional.of(draft));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "PUBLISHED"))
        .thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_static_pages")).thenReturn(List.of(draft));
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> published = service.publish("patient_static_pages", "publish", 1L);

    assertEquals("PUBLISHED", published.get("status"));
  }

  @Test
  void rejectsPublishWithMissingRouteNameInNavLinks() {
    PatientSiteConfig draft = config("patient_nav", "DRAFT", 1,
        "{\"brand\":{\"name\":\"智慧云脑\",\"homeRoute\":\"patient-home\"},\"menus\":[{\"key\":\"care\",\"label\":\"Care\",\"links\":[{\"label\":\"Book\"}]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_nav", "publish", 1L));
  }

  @Test
  void rejectsPublishWithUnknownHomeModule() {
    PatientSiteConfig draft = config("patient_home", "DRAFT", 1,
        "{\"hero\":{\"title\":\"Home\"},\"modules\":[{\"type\":\"custom\",\"key\":\"x\"}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_home", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_home", "publish", 1L));
  }

  @Test
  void publishesPatientPagesConfigWithAllowedSections() {
    PatientSiteConfig draft = config("patient_pages", "DRAFT", 1,
        "{\"pages\":[{\"routeName\":\"about-hospital\",\"slug\":\"about-hospital\",\"label\":\"CMS\",\"title\":\"CMS\",\"intro\":\"Intro\",\"sections\":[{\"type\":\"notice\",\"text\":\"Notice\"},{\"type\":\"rich_text\",\"body\":\"Body\"},{\"type\":\"link_grid\",\"links\":[{\"label\":\"Contact\",\"routeName\":\"about-contact\"},{\"label\":\"CMS\",\"routeName\":\"cms-page\",\"slug\":\"about-hospital\"}]}]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "DRAFT"))
        .thenReturn(Optional.of(draft));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "PUBLISHED"))
        .thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_pages")).thenReturn(List.of(draft));
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> published = service.publish("patient_pages", "publish", 1L);

    assertEquals("PUBLISHED", published.get("status"));
  }

  @Test
  void rejectsPatientPagesConfigWithUnknownSectionType() {
    PatientSiteConfig draft = config("patient_pages", "DRAFT", 1,
        "{\"pages\":[{\"routeName\":\"about-hospital\",\"label\":\"关于智慧云脑\",\"title\":\"医院介绍\",\"sections\":[{\"type\":\"unsafe_script\",\"body\":\"bad\"}]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_pages", "publish", 1L));
  }

  @Test
  void rejectsCmsPageRouteTargetWithoutSlug() {
    PatientSiteConfig draft = config("patient_pages", "DRAFT", 1,
        "{\"pages\":[{\"routeName\":\"about-hospital\",\"slug\":\"about-hospital\",\"label\":\"CMS\",\"title\":\"CMS\",\"sections\":[{\"type\":\"link_grid\",\"links\":[{\"label\":\"CMS\",\"routeName\":\"cms-page\"}]}]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_pages", "publish", 1L));
  }

  @Test
  void publishesDraftArchivesOldPublishedAndPublicReadsPublishedOnly() {
    PatientSiteConfig draft = config("patient_home", "DRAFT", 2,
        "{\"hero\":{\"title\":\"Home\",\"primaryAction\":{\"label\":\"Book\",\"routeName\":\"patient-doctors\"}},\"modules\":[{\"type\":\"notice\",\"key\":\"notice\"}]}");
    draft.setCreatedBy(3L);
    PatientSiteConfig old = config("patient_home", "PUBLISHED", 1, "{\"hero\":{\"title\":\"Old\"},\"modules\":[]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_home", "DRAFT"))
        .thenReturn(Optional.of(draft));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_home", "PUBLISHED"))
        .thenReturn(List.of(old));
    when(repository.findByConfigKeyOrderByVersionDesc("patient_home")).thenReturn(List.of(draft, old));
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> published = service.publish("patient_home", "publish", 7L);

    assertEquals("PUBLISHED", published.get("status"));
    assertEquals(3, published.get("version"));
    assertEquals(3L, published.get("createdBy"));
    assertEquals(7L, published.get("updatedBy"));
    assertEquals("ARCHIVED", old.getStatus());
    assertEquals(7L, old.getUpdatedBy());
    assertEquals("ARCHIVED", draft.getStatus());
    assertEquals(7L, draft.getUpdatedBy());

    ArgumentCaptor<PatientSiteConfig> captor = ArgumentCaptor.forClass(PatientSiteConfig.class);
    verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

    PatientSiteConfig publishedConfig = config("patient_home", "PUBLISHED", 3,
        "{\"hero\":{\"title\":\"Home\"},\"modules\":[]}");
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_home", "PUBLISHED")).thenReturn(List.of(publishedConfig));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_hospital_info", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_footer", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_nav")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_static_pages")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_pages")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_hospital_info")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_footer")).thenReturn(List.of());

    @SuppressWarnings("unchecked")
    Map<String, Object> home = (Map<String, Object>) service.publicConfig().get("home");
    @SuppressWarnings("unchecked")
    Map<String, Object> hero = (Map<String, Object>) home.get("hero");
    assertEquals("Home", hero.get("title"));
  }

  @Test
  void publicConfigSkipsInvalidPublishedRows() {
    PatientSiteConfig invalid = config("patient_home", "PUBLISHED", 3, "12345");
    PatientSiteConfig valid = config("patient_home", "PUBLISHED", 2, "{\"hero\":{\"title\":\"Fallback\"},\"modules\":[]}");
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_home", "PUBLISHED")).thenReturn(List.of(invalid, valid));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_hospital_info", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_footer", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_nav")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_static_pages")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_pages")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_hospital_info")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_footer")).thenReturn(List.of());

    @SuppressWarnings("unchecked")
    Map<String, Object> home = (Map<String, Object>) service.publicConfig().get("home");
    @SuppressWarnings("unchecked")
    Map<String, Object> hero = (Map<String, Object>) home.get("hero");
    assertEquals("Fallback", hero.get("title"));
  }

  @Test
  void publicConfigInitializesMissingPublishedDefaults() {
    List<PatientSiteConfig> stored = new ArrayList<>();
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc(any(), any())).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      String status = invocation.getArgument(1);
      return stored.stream()
          .filter(config -> key.equals(config.getConfigKey()) && status.equals(config.getStatus()))
          .toList();
    });
    when(repository.findByConfigKeyOrderByVersionDesc(any())).thenAnswer(invocation -> {
      String key = invocation.getArgument(0);
      return stored.stream().filter(config -> key.equals(config.getConfigKey())).toList();
    });
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> {
      PatientSiteConfig value = invocation.getArgument(0);
      stored.add(value);
      return value;
    });

    Map<String, Object> config = service.publicConfig();

    assertEquals(6, stored.size());
    assertEquals(6, stored.stream().map(PatientSiteConfig::getConfigKey).distinct().count());
    assertTrue(stored.stream().allMatch(row -> "PUBLISHED".equals(row.getStatus())));
    assertTrue(((Map<?, ?>) config.get("nav")).containsKey("brand"));
    assertTrue(((Map<?, ?>) config.get("home")).containsKey("hero"));
    assertTrue(((Map<?, ?>) config.get("staticPages")).containsKey("pages"));
    assertTrue(((Map<?, ?>) config.get("pages")).containsKey("pages"));
    assertTrue(((Map<?, ?>) config.get("hospitalInfo")).containsKey("name"));
    assertTrue(((Map<?, ?>) config.get("footer")).containsKey("brandName"));
  }

  @Test
  void previewTokenReadsSelectedVersionOverPublishedConfig() {
    PatientSiteConfig publishedPages = config("patient_pages", "PUBLISHED", 1, "{\"pages\":[]}");
    PatientSiteConfig draftPages = config("patient_pages", "DRAFT", 2,
        "{\"pages\":[{\"routeName\":\"about-hospital\",\"slug\":\"draft-page\",\"label\":\"Draft\",\"title\":\"Draft\",\"sections\":[]}]}");
    when(repository.findByConfigKeyOrderByVersionDesc("patient_pages")).thenReturn(List.of(draftPages, publishedPages));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_home", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_pages", "PUBLISHED")).thenReturn(List.of(publishedPages));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_hospital_info", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_footer", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_nav")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_home")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_static_pages")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_hospital_info")).thenReturn(List.of());
    when(repository.findByConfigKeyOrderByVersionDesc("patient_footer")).thenReturn(List.of());
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> token = service.createPreviewToken("patient_pages", 2);
    Map<String, Object> preview = service.previewConfig(String.valueOf(token.get("token")));

    @SuppressWarnings("unchecked")
    Map<String, Object> pages = (Map<String, Object>) preview.get("pages");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> pageList = (List<Map<String, Object>>) pages.get("pages");
    assertEquals("draft-page", pageList.get(0).get("slug"));
  }

  @Test
  void rejectsInvalidPreviewToken() {
    assertThrows(BusinessException.class, () -> service.previewConfig("bad.token"));
  }

  private PatientSiteConfig config(String key, String status, int version, String json) {
    PatientSiteConfig config = new PatientSiteConfig();
    config.setConfigKey(key);
    config.setStatus(status);
    config.setVersion(version);
    config.setConfigJson(json);
    return config;
  }
}
