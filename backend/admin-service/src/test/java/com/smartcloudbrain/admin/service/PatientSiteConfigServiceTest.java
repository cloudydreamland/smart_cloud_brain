package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigSaveRequest;
import com.smartcloudbrain.admin.entity.PatientSiteConfig;
import com.smartcloudbrain.admin.repository.PatientSiteConfigRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientSiteConfigServiceTest {

  @Mock private PatientSiteConfigRepository repository;
  private PatientSiteConfigService service;

  @BeforeEach
  void setUp() {
    service = new PatientSiteConfigService(repository, new ObjectMapper());
  }

  @Test
  void savesValidDraftConfig() {
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
  }

  @Test
  void rejectsInvalidJsonAndUnknownKeyOnSave() {
    assertThrows(BusinessException.class, () -> service.saveDraft(
        new PatientSiteConfigSaveRequest("patient_nav", "{bad", null), 1L));
    assertThrows(BusinessException.class, () -> service.saveDraft(
        new PatientSiteConfigSaveRequest("unknown", "{}", null), 1L));
  }

  @Test
  void rejectsPublishWithUnknownRouteName() {
    PatientSiteConfig draft = config("patient_static_pages", "DRAFT", 1,
        "{\"pages\":[{\"routeName\":\"bad-route\",\"title\":\"Bad\",\"points\":[]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_static_pages", 1L));
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

    Map<String, Object> published = service.publish("patient_static_pages", 1L);

    assertEquals("PUBLISHED", published.get("status"));
  }

  @Test
  void rejectsPublishWithMissingRouteNameInNavLinks() {
    PatientSiteConfig draft = config("patient_nav", "DRAFT", 1,
        "{\"brand\":{\"name\":\"智慧云脑\",\"homeRoute\":\"patient-home\"},\"menus\":[{\"key\":\"care\",\"label\":\"Care\",\"links\":[{\"label\":\"Book\"}]}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_nav", 1L));
  }

  @Test
  void rejectsPublishWithUnknownHomeModule() {
    PatientSiteConfig draft = config("patient_home", "DRAFT", 1,
        "{\"hero\":{\"title\":\"Home\"},\"modules\":[{\"type\":\"custom\",\"key\":\"x\"}]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_home", "DRAFT"))
        .thenReturn(Optional.of(draft));

    assertThrows(BusinessException.class, () -> service.publish("patient_home", 1L));
  }

  @Test
  void publishesDraftArchivesOldPublishedAndPublicReadsPublishedOnly() {
    PatientSiteConfig draft = config("patient_home", "DRAFT", 2,
        "{\"hero\":{\"title\":\"Home\",\"primaryAction\":{\"label\":\"Book\",\"routeName\":\"patient-doctors\"}},\"modules\":[{\"type\":\"notice\",\"key\":\"notice\"}]}");
    PatientSiteConfig old = config("patient_home", "PUBLISHED", 1, "{\"hero\":{\"title\":\"Old\"},\"modules\":[]}");
    when(repository.findFirstByConfigKeyAndStatusOrderByVersionDesc("patient_home", "DRAFT"))
        .thenReturn(Optional.of(draft));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_home", "PUBLISHED"))
        .thenReturn(List.of(old));
    when(repository.findByConfigKeyOrderByVersionDesc("patient_home")).thenReturn(List.of(draft, old));
    when(repository.save(any(PatientSiteConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> published = service.publish("patient_home", 7L);

    assertEquals("PUBLISHED", published.get("status"));
    assertEquals(3, published.get("version"));
    assertEquals("ARCHIVED", old.getStatus());
    assertEquals("ARCHIVED", draft.getStatus());

    ArgumentCaptor<PatientSiteConfig> captor = ArgumentCaptor.forClass(PatientSiteConfig.class);
    verify(repository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

    PatientSiteConfig publishedConfig = config("patient_home", "PUBLISHED", 3,
        "{\"hero\":{\"title\":\"Home\"},\"modules\":[]}");
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_nav", "PUBLISHED")).thenReturn(List.of());
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_home", "PUBLISHED")).thenReturn(List.of(publishedConfig));
    when(repository.findByConfigKeyAndStatusOrderByVersionDesc("patient_static_pages", "PUBLISHED")).thenReturn(List.of());

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

    @SuppressWarnings("unchecked")
    Map<String, Object> home = (Map<String, Object>) service.publicConfig().get("home");
    @SuppressWarnings("unchecked")
    Map<String, Object> hero = (Map<String, Object>) home.get("hero");
    assertEquals("Fallback", hero.get("title"));
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
