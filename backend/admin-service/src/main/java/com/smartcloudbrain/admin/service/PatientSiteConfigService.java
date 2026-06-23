package com.smartcloudbrain.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigSaveRequest;
import com.smartcloudbrain.admin.entity.PatientSiteConfig;
import com.smartcloudbrain.admin.repository.PatientSiteConfigRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientSiteConfigService {

  private static final String STATUS_DRAFT = "DRAFT";
  private static final String STATUS_PUBLISHED = "PUBLISHED";
  private static final String STATUS_ARCHIVED = "ARCHIVED";
  private static final Set<String> CONFIG_KEYS = Set.of("patient_nav", "patient_home", "patient_static_pages");
  private static final Set<String> ROUTES = Set.of(
      "patient-home", "patient-dashboard", "patient-triage", "patient-doctors", "patient-appointments",
      "patient-records", "patient-prescriptions", "patient-reports", "patient-invoices", "patient-messages",
      "patient-profile", "patient-visitors", "public-search", "public-departments", "public-conditions",
      "public-guide", "public-research", "service-internet-clinic", "service-exam-booking", "service-inpatient",
      "service-emergency", "service-international", "doctor-experts", "doctor-centers", "doctor-schedules",
      "library-symptoms", "library-drugs", "library-tests", "library-rehab", "library-articles",
      "ai-symptom", "ai-record-summary", "ai-medication", "ai-assessment", "about-hospital",
      "about-news", "about-careers", "about-contact"
  );
  private static final Set<String> HOME_MODULES = Set.of(
      "notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"
  );

  private final PatientSiteConfigRepository repository;
  private final ObjectMapper objectMapper;

  public PatientSiteConfigService(PatientSiteConfigRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> latest(String configKey) {
    if (isBlank(configKey)) {
      Map<String, Object> view = new LinkedHashMap<>();
      for (String key : CONFIG_KEYS.stream().sorted().toList()) {
        view.put(key, repository.findByConfigKeyOrderByVersionDesc(key).stream().findFirst().map(this::view).orElse(Map.of()));
      }
      return view;
    }
    requireConfigKey(configKey);
    return repository.findByConfigKeyOrderByVersionDesc(configKey).stream().findFirst().map(this::view).orElse(Map.of());
  }

  public List<Map<String, Object>> history(String configKey) {
    requireConfigKey(configKey);
    return repository.findByConfigKeyOrderByVersionDesc(configKey).stream().map(this::view).toList();
  }

  @Transactional
  public Map<String, Object> saveDraft(PatientSiteConfigSaveRequest request, Long userId) {
    String key = requireConfigKey(request.configKey());
    JsonNode root = parseObject(request.configJson());
    int version = nextVersion(key);
    PatientSiteConfig config = new PatientSiteConfig();
    config.setConfigKey(key);
    config.setConfigJson(compact(root));
    config.setStatus(STATUS_DRAFT);
    config.setVersion(version);
    config.setRemark(request.remark());
    config.setCreatedBy(userId);
    config.setUpdatedBy(userId);
    config.setUpdatedAt(LocalDateTime.now());
    return view(repository.save(config));
  }

  @Transactional
  public Map<String, Object> publish(String configKey, Long userId) {
    String key = requireConfigKey(configKey);
    PatientSiteConfig draft = repository.findFirstByConfigKeyAndStatusOrderByVersionDesc(key, STATUS_DRAFT)
        .orElseThrow(() -> new BusinessException(400, "Draft config not found: " + key));
    JsonNode root = parseObject(draft.getConfigJson());
    validateStructure(key, root);

    for (PatientSiteConfig old : repository.findByConfigKeyAndStatusOrderByVersionDesc(key, STATUS_PUBLISHED)) {
      old.setStatus(STATUS_ARCHIVED);
      old.setUpdatedBy(userId);
      old.setUpdatedAt(LocalDateTime.now());
      repository.save(old);
    }
    draft.setStatus(STATUS_ARCHIVED);
    draft.setUpdatedBy(userId);
    draft.setUpdatedAt(LocalDateTime.now());
    repository.save(draft);

    PatientSiteConfig published = new PatientSiteConfig();
    published.setConfigKey(key);
    published.setConfigJson(compact(root));
    published.setStatus(STATUS_PUBLISHED);
    published.setVersion(nextVersion(key));
    published.setRemark(draft.getRemark());
    published.setCreatedBy(draft.getCreatedBy());
    published.setUpdatedBy(userId);
    published.setUpdatedAt(LocalDateTime.now());
    return view(repository.save(published));
  }

  public Map<String, Object> publicConfig() {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("nav", publishedJson("patient_nav"));
    view.put("home", publishedJson("patient_home"));
    view.put("staticPages", publishedJson("patient_static_pages"));
    return view;
  }

  private Object publishedJson(String configKey) {
    return repository.findByConfigKeyAndStatusOrderByVersionDesc(configKey, STATUS_PUBLISHED).stream()
        .map(PatientSiteConfig::getConfigJson)
        .map(this::tryParseObject)
        .filter(node -> node != null)
        .findFirst()
        .map(node -> objectMapper.convertValue(node, Object.class))
        .orElseGet(LinkedHashMap::new);
  }

  private int nextVersion(String configKey) {
    return repository.findByConfigKeyOrderByVersionDesc(configKey).stream()
        .map(PatientSiteConfig::getVersion)
        .filter(version -> version != null)
        .max(Comparator.naturalOrder())
        .orElse(0) + 1;
  }

  private String requireConfigKey(String configKey) {
    String key = configKey == null ? "" : configKey.trim();
    if (!CONFIG_KEYS.contains(key)) {
      throw new BusinessException(400, "Unsupported patient site config key: " + configKey);
    }
    return key;
  }

  private JsonNode parseObject(String configJson) {
    if (isBlank(configJson)) {
      throw new BusinessException(400, "configJson is required");
    }
    try {
      JsonNode root = objectMapper.readTree(configJson);
      if (!root.isObject()) {
        throw new BusinessException(400, "configJson must be a JSON object");
      }
      return root;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(400, "configJson must be valid JSON");
    }
  }

  private JsonNode tryParseObject(String configJson) {
    if (isBlank(configJson)) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(configJson);
      return root.isObject() ? root : null;
    } catch (Exception ex) {
      return null;
    }
  }

  private void validateStructure(String configKey, JsonNode root) {
    validateRoutes(root);
    switch (configKey) {
      case "patient_nav" -> validateNav(root);
      case "patient_home" -> validateHome(root);
      case "patient_static_pages" -> validateStaticPages(root);
      default -> throw new BusinessException(400, "Unsupported patient site config key: " + configKey);
    }
  }

  private void validateNav(JsonNode root) {
    JsonNode brand = root.path("brand");
    if (brand.isObject() && isBlank(brand.path("name").asText(""))) {
      throw new BusinessException(400, "patient_nav.brand.name is required");
    }
    if (brand.isObject() && !isBlank(brand.path("homeRoute").asText(""))) {
      requireAllowedRoute(brand.path("homeRoute").asText(), "patient_nav.brand.homeRoute");
    }
    for (JsonNode menu : array(root.path("menus"), "patient_nav.menus")) {
      requireText(menu, "key", "patient_nav.menus[].key");
      requireText(menu, "label", "patient_nav.menus[].label");
      JsonNode links = menu.path("links");
      if (links.isArray()) {
        for (JsonNode link : links) {
          requireText(link, "label", "patient_nav.menus[].links[].label");
          requireRouteName(link, "patient_nav.menus[].links[].routeName");
        }
      }
      if (menu.path("feature").isObject()) {
        requireText(menu.path("feature"), "label", "patient_nav.menus[].feature.label");
        requireRouteName(menu.path("feature"), "patient_nav.menus[].feature.routeName");
      }
    }
    for (JsonNode link : array(root.path("userLinks"), "patient_nav.userLinks")) {
      requireText(link, "label", "patient_nav.userLinks[].label");
      requireRouteName(link, "patient_nav.userLinks[].routeName");
    }
  }

  private void validateHome(JsonNode root) {
    JsonNode hero = root.path("hero");
    if (hero.isObject() && hero.path("enabled").asBoolean(true)) {
      requireText(hero, "title", "patient_home.hero.title");
      validateOptionalAction(hero.path("primaryAction"), "patient_home.hero.primaryAction");
      validateOptionalAction(hero.path("secondaryAction"), "patient_home.hero.secondaryAction");
    }
    for (JsonNode module : array(root.path("modules"), "patient_home.modules")) {
      String type = requireText(module, "type", "patient_home.modules[].type");
      if (!HOME_MODULES.contains(type)) {
        throw new BusinessException(400, "Unsupported patient home module type: " + type);
      }
      requireText(module, "key", "patient_home.modules[].key");
      if ("quick_actions".equals(type)) {
        JsonNode items = module.path("content").path("items");
        for (JsonNode item : array(items, "patient_home.modules[].content.items")) {
          requireText(item, "label", "patient_home.modules[].content.items[].label");
          requireRouteName(item, "patient_home.modules[].content.items[].routeName");
        }
      }
    }
  }

  private void validateStaticPages(JsonNode root) {
    for (JsonNode page : array(root.path("pages"), "patient_static_pages.pages")) {
      requireText(page, "routeName", "patient_static_pages.pages[].routeName");
      requireText(page, "title", "patient_static_pages.pages[].title");
      if (page.path("primary").isObject()) {
        requireText(page.path("primary"), "label", "patient_static_pages.pages[].primary.label");
        requireRouteName(page.path("primary"), "patient_static_pages.pages[].primary.routeName");
      }
      if (page.path("points").isArray()) {
        for (JsonNode point : page.path("points")) {
          requireText(point, "title", "patient_static_pages.pages[].points[].title");
          requireText(point, "text", "patient_static_pages.pages[].points[].text");
        }
      }
    }
  }

  private List<JsonNode> array(JsonNode node, String path) {
    if (node.isMissingNode() || node.isNull()) {
      return List.of();
    }
    if (!node.isArray()) {
      throw new BusinessException(400, path + " must be an array");
    }
    List<JsonNode> values = new ArrayList<>();
    node.forEach(values::add);
    return values;
  }

  private String requireText(JsonNode node, String field, String path) {
    String value = node.path(field).asText("");
    if (isBlank(value)) {
      throw new BusinessException(400, path + " is required");
    }
    return value.trim();
  }

  private void validateOptionalAction(JsonNode action, String path) {
    if (!action.isObject()) {
      return;
    }
    requireText(action, "label", path + ".label");
    requireRouteName(action, path + ".routeName");
  }

  private void requireRouteName(JsonNode node, String path) {
    String routeName = requireText(node, "routeName", path);
    requireAllowedRoute(routeName, path);
  }

  private void requireAllowedRoute(String routeName, String path) {
    if (!ROUTES.contains(routeName)) {
      throw new BusinessException(400, "Unsupported patient routeName at " + path + ": " + routeName);
    }
  }

  private void validateRoutes(JsonNode root) {
    List<String> routeNames = new ArrayList<>();
    collectRouteNames(root, routeNames);
    for (String routeName : routeNames) {
      if (!ROUTES.contains(routeName)) {
        throw new BusinessException(400, "Unsupported patient routeName: " + routeName);
      }
    }
  }

  private void collectRouteNames(JsonNode node, List<String> routeNames) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isObject()) {
      node.fields().forEachRemaining(entry -> {
        if ("routeName".equals(entry.getKey()) && entry.getValue().isTextual()) {
          routeNames.add(entry.getValue().asText());
        }
        collectRouteNames(entry.getValue(), routeNames);
      });
    } else if (node.isArray()) {
      node.forEach(child -> collectRouteNames(child, routeNames));
    }
  }

  private Map<String, Object> view(PatientSiteConfig config) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", config.getId());
    view.put("configKey", config.getConfigKey());
    view.put("configJson", config.getConfigJson());
    view.put("status", config.getStatus());
    view.put("version", config.getVersion());
    view.put("remark", config.getRemark() == null ? "" : config.getRemark());
    view.put("createdBy", config.getCreatedBy());
    view.put("updatedBy", config.getUpdatedBy());
    view.put("createdAt", config.getCreatedAt() == null ? "" : config.getCreatedAt().toString());
    view.put("updatedAt", config.getUpdatedAt() == null ? "" : config.getUpdatedAt().toString());
    return view;
  }

  private String compact(JsonNode root) {
    try {
      return objectMapper.writeValueAsString(root);
    } catch (Exception ex) {
      throw new BusinessException(400, "configJson must be serializable JSON");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
