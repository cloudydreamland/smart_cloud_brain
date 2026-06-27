package com.smartcloudbrain.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloudbrain.admin.dto.admin.PatientSiteConfigSaveRequest;
import com.smartcloudbrain.admin.entity.PatientSiteConfig;
import com.smartcloudbrain.admin.repository.PatientSiteConfigRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientSiteConfigService {

  private static final String STATUS_DRAFT = "DRAFT";
  private static final String STATUS_PUBLISHED = "PUBLISHED";
  private static final String STATUS_ARCHIVED = "ARCHIVED";
  private static final int HISTORY_RETENTION_LIMIT = 50;
  private static final int MAX_HISTORY_PAGE_SIZE = 100;
  private static final Set<String> CONFIG_KEYS = Set.of("patient_nav", "patient_home", "patient_static_pages", "patient_pages");
  private static final Set<String> ROUTES = Set.of(
      "patient-home", "patient-dashboard", "patient-triage", "patient-doctors", "patient-appointments",
      "patient-records", "patient-prescriptions", "patient-reports", "patient-invoices", "patient-messages",
      "patient-profile", "patient-visitors", "public-search", "public-departments", "public-conditions",
      "public-guide", "public-research", "service-internet-clinic", "service-exam-booking", "service-inpatient",
      "service-emergency", "service-international", "doctor-experts", "doctor-centers", "doctor-schedules",
      "library-symptoms", "library-drugs", "library-tests", "library-rehab", "library-articles",
      "ai-symptom", "ai-record-summary", "ai-medication", "ai-assessment", "about-hospital",
      "about-news", "about-careers", "about-contact", "cms-page"
  );
  private static final Set<String> HOME_MODULES = Set.of(
      "notice", "quick_actions", "intro", "locations", "featured_departments", "static_content"
  );
  private static final Set<String> SECTION_TYPES = Set.of(
      "notice", "rich_text", "card_grid", "faq", "timeline", "cta", "link_grid", "department_links"
  );

  private final PatientSiteConfigRepository repository;
  private final ObjectMapper objectMapper;
  private final byte[] previewSecret;
  private JsonNode defaultConfig;

  public PatientSiteConfigService(
      PatientSiteConfigRepository repository,
      ObjectMapper objectMapper,
      @Value("${patient-site.preview-secret:${JWT_SECRET:smart-cloud-brain-local-secret-please-change}}") String previewSecret
  ) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.previewSecret = previewSecret.getBytes(StandardCharsets.UTF_8);
  }

  @Transactional
  public Map<String, Object> latest(String configKey) {
    if (isBlank(configKey)) {
      ensurePublishedDefaults();
      Map<String, Object> view = new LinkedHashMap<>();
      for (String key : CONFIG_KEYS.stream().sorted().toList()) {
        view.put(key, repository.findByConfigKeyOrderByVersionDesc(key).stream().findFirst().map(this::view).orElse(Map.of()));
      }
      return view;
    }
    requireConfigKey(configKey);
    ensurePublishedDefault(configKey);
    return repository.findByConfigKeyOrderByVersionDesc(configKey).stream().findFirst().map(this::view).orElse(Map.of());
  }

  @Transactional
  public Map<String, Object> history(String configKey, int page, int pageSize) {
    requireConfigKey(configKey);
    ensurePublishedDefault(configKey);
    int normalizedPage = normalizePage(page);
    int normalizedPageSize = normalizePageSize(pageSize);
    Page<PatientSiteConfig> rows = repository.findByConfigKeyOrderByVersionDesc(
        configKey,
        PageRequest.of(normalizedPage - 1, normalizedPageSize)
    );
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", rows.getContent().stream().map(this::view).toList());
    result.put("page", normalizedPage);
    result.put("pageSize", normalizedPageSize);
    result.put("total", rows.getTotalElements());
    result.put("totalPages", Math.max(1, rows.getTotalPages()));
    return result;
  }

  @Transactional
  public Map<String, Object> saveDraft(PatientSiteConfigSaveRequest request, Long userId) {
    String key = requireConfigKey(request.configKey());
    JsonNode root = parseObject(request.configJson());
    PatientSiteConfig config = repository.findFirstByConfigKeyAndStatusOrderByVersionDesc(key, STATUS_DRAFT)
        .orElseGet(() -> newDraft(key, userId));
    config.setConfigKey(key);
    config.setConfigJson(compact(root));
    config.setRemark(request.remark());
    config.setUpdatedBy(userId);
    config.setUpdatedAt(LocalDateTime.now());
    PatientSiteConfig saved = repository.save(config);
    enforceHistoryRetention(key);
    return view(saved);
  }

  @Transactional
  public Map<String, Object> savePublished(PatientSiteConfigSaveRequest request, Long userId) {
    String key = requireConfigKey(request.configKey());
    JsonNode root = parseObject(request.configJson());
    validateStructure(key, root);
    return createPublished(key, root, requireRemark(request.remark()), userId, userId);
  }

  @Transactional
  public Map<String, Object> publish(String configKey, String remark, Long userId) {
    String key = requireConfigKey(configKey);
    String publishRemark = requireRemark(remark);
    PatientSiteConfig draft = repository.findFirstByConfigKeyAndStatusOrderByVersionDesc(key, STATUS_DRAFT)
        .orElseThrow(() -> new BusinessException(400, "Draft config not found: " + key));
    JsonNode root = parseObject(draft.getConfigJson());
    validateStructure(key, root);

    draft.setStatus(STATUS_ARCHIVED);
    draft.setUpdatedBy(userId);
    draft.setUpdatedAt(LocalDateTime.now());
    repository.save(draft);

    return createPublished(key, root, publishRemark, draft.getCreatedBy(), userId);
  }

  private Map<String, Object> createPublished(String key, JsonNode root, String remark, Long createdBy, Long userId) {
    for (PatientSiteConfig old : repository.findByConfigKeyAndStatusOrderByVersionDesc(key, STATUS_PUBLISHED)) {
      old.setStatus(STATUS_ARCHIVED);
      old.setUpdatedBy(userId);
      old.setUpdatedAt(LocalDateTime.now());
      repository.save(old);
    }

    PatientSiteConfig published = new PatientSiteConfig();
    published.setConfigKey(key);
    published.setConfigJson(compact(root));
    published.setStatus(STATUS_PUBLISHED);
    published.setVersion(nextVersion(key));
    published.setRemark(remark);
    published.setCreatedBy(createdBy);
    published.setUpdatedBy(userId);
    published.setUpdatedAt(LocalDateTime.now());
    PatientSiteConfig saved = repository.save(published);
    enforceHistoryRetention(key);
    return view(saved);
  }

  private PatientSiteConfig newDraft(String key, Long userId) {
    PatientSiteConfig config = new PatientSiteConfig();
    config.setConfigKey(key);
    config.setStatus(STATUS_DRAFT);
    config.setVersion(nextVersion(key));
    config.setCreatedBy(userId);
    return config;
  }

  private void enforceHistoryRetention(String key) {
    long overflow = repository.countByConfigKey(key) - HISTORY_RETENTION_LIMIT;
    if (overflow <= 0) {
      return;
    }
    int deleteCount = overflow > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) overflow;
    List<PatientSiteConfig> archived = repository.findByConfigKeyAndStatusOrderByVersionAsc(
        key,
        STATUS_ARCHIVED,
        PageRequest.of(0, deleteCount)
    );
    if (!archived.isEmpty()) {
      repository.deleteAll(archived);
    }
  }

  @Transactional
  public Map<String, Object> publicConfig() {
    ensurePublishedDefaults();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("nav", publishedJson("patient_nav"));
    view.put("home", publishedJson("patient_home"));
    view.put("staticPages", publishedJson("patient_static_pages"));
    view.put("pages", publishedJson("patient_pages"));
    return view;
  }

  @Transactional
  public Map<String, Object> createPreviewToken(String configKey, Integer version) {
    String key = requireConfigKey(configKey);
    PatientSiteConfig preview = selectPreviewConfig(key, version);
    long expiresAt = Instant.now().plusSeconds(15 * 60).getEpochSecond();
    String payload = key + "|" + preview.getVersion() + "|" + expiresAt;
    String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    String signature = sign(encodedPayload);
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("token", encodedPayload + "." + signature);
    view.put("configKey", key);
    view.put("version", preview.getVersion());
    view.put("expiresAt", expiresAt);
    return view;
  }

  @Transactional
  public Map<String, Object> previewConfig(String token) {
    PreviewPayload payload = parsePreviewToken(token);
    PatientSiteConfig preview = repository.findByConfigKeyOrderByVersionDesc(payload.configKey()).stream()
        .filter(row -> Integer.valueOf(payload.version()).equals(row.getVersion()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Preview config version not found"));
    JsonNode override = parseObject(preview.getConfigJson());
    Map<String, Object> view = publicConfig();
    view.put(publicField(payload.configKey()), objectMapper.convertValue(override, Object.class));
    return view;
  }

  private void ensurePublishedDefaults() {
    for (String key : CONFIG_KEYS) {
      ensurePublishedDefault(key);
    }
  }

  private void ensurePublishedDefault(String configKey) {
    String key = requireConfigKey(configKey);
    if (!repository.findByConfigKeyAndStatusOrderByVersionDesc(key, STATUS_PUBLISHED).isEmpty()) {
      return;
    }
    JsonNode root = loadDefaultSection(key);
    validateStructure(key, root);

    PatientSiteConfig config = new PatientSiteConfig();
    config.setConfigKey(key);
    config.setConfigJson(compact(root));
    config.setStatus(STATUS_PUBLISHED);
    config.setVersion(nextVersion(key));
    config.setRemark("系统默认配置初始化");
    config.setUpdatedAt(LocalDateTime.now());
    repository.save(config);
  }

  private JsonNode loadDefaultSection(String configKey) {
    JsonNode root = defaultConfig();
    String section = switch (configKey) {
      case "patient_nav" -> "nav";
      case "patient_home" -> "home";
      case "patient_static_pages" -> "staticPages";
      case "patient_pages" -> "pages";
      default -> throw new BusinessException(400, "Unsupported patient site config key: " + configKey);
    };
    if ("patient_pages".equals(configKey) && !root.has(section)) {
      return objectMapper.createObjectNode().set("pages", objectMapper.createArrayNode());
    }
    JsonNode value = root.path(section);
    if (!value.isObject()) {
      throw new BusinessException(500, "Default patient site config section missing: " + section);
    }
    return value;
  }

  private PatientSiteConfig selectPreviewConfig(String configKey, Integer version) {
    List<PatientSiteConfig> configs = repository.findByConfigKeyOrderByVersionDesc(configKey);
    if (version != null) {
      return configs.stream()
          .filter(row -> version.equals(row.getVersion()))
          .findFirst()
          .orElseThrow(() -> new BusinessException(404, "Preview config version not found: " + version));
    }
    return configs.stream()
        .filter(row -> STATUS_DRAFT.equals(row.getStatus()))
        .findFirst()
        .or(() -> configs.stream().findFirst())
        .orElseThrow(() -> new BusinessException(404, "Preview config not found: " + configKey));
  }

  private String publicField(String configKey) {
    return switch (configKey) {
      case "patient_nav" -> "nav";
      case "patient_home" -> "home";
      case "patient_static_pages" -> "staticPages";
      case "patient_pages" -> "pages";
      default -> throw new BusinessException(400, "Unsupported patient site config key: " + configKey);
    };
  }

  private JsonNode defaultConfig() {
    if (defaultConfig != null) {
      return defaultConfig;
    }
    try (InputStream input = PatientSiteConfigService.class.getResourceAsStream("/patient-site-defaults.json")) {
      if (input == null) {
        throw new BusinessException(500, "Default patient site config resource not found");
      }
      JsonNode root = objectMapper.readTree(input);
      if (!root.isObject()) {
        throw new BusinessException(500, "Default patient site config must be a JSON object");
      }
      defaultConfig = root;
      return root;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(500, "Default patient site config is invalid");
    }
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

  private int normalizePage(int page) {
    return Math.max(1, page);
  }

  private int normalizePageSize(int pageSize) {
    return Math.min(Math.max(1, pageSize), MAX_HISTORY_PAGE_SIZE);
  }

  private String requireConfigKey(String configKey) {
    String key = configKey == null ? "" : configKey.trim();
    if (!CONFIG_KEYS.contains(key)) {
      throw new BusinessException(400, "Unsupported patient site config key: " + configKey);
    }
    return key;
  }

  private String requireRemark(String remark) {
    if (isBlank(remark)) {
      throw new BusinessException(400, "Publish remark is required");
    }
    return remark.trim();
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
      case "patient_pages" -> validatePages(root);
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

  private void validatePages(JsonNode root) {
    for (JsonNode page : array(root.path("pages"), "patient_pages.pages")) {
      requireText(page, "routeName", "patient_pages.pages[].routeName");
      requireAllowedRoute(page.path("routeName").asText(), "patient_pages.pages[].routeName");
      requireText(page, "label", "patient_pages.pages[].label");
      requireText(page, "title", "patient_pages.pages[].title");
      String slug = page.path("slug").asText("");
      if (!isBlank(slug) && !slug.matches("^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$")) {
        throw new BusinessException(400, "patient_pages.pages[].slug is invalid: " + slug);
      }
      for (JsonNode section : array(page.path("sections"), "patient_pages.pages[].sections")) {
        String type = requireText(section, "type", "patient_pages.pages[].sections[].type");
        if (!SECTION_TYPES.contains(type)) {
          throw new BusinessException(400, "Unsupported patient page section type: " + type);
        }
        validateSection(section, type);
      }
    }
  }

  private void validateSection(JsonNode section, String type) {
    switch (type) {
      case "notice" -> requireText(section, "text", "patient_pages.pages[].sections[].text");
      case "rich_text" -> requireText(section, "body", "patient_pages.pages[].sections[].body");
      case "card_grid" -> validateCards(section.path("cards"), "patient_pages.pages[].sections[].cards");
      case "faq" -> validateFaqItems(section.path("items"), "patient_pages.pages[].sections[].items");
      case "timeline" -> validateTimelineItems(section.path("items"), "patient_pages.pages[].sections[].items");
      case "cta" -> {
        requireText(section, "text", "patient_pages.pages[].sections[].text");
        validateOptionalAction(section.path("primary"), "patient_pages.pages[].sections[].primary");
        validateOptionalAction(section.path("secondary"), "patient_pages.pages[].sections[].secondary");
      }
      case "link_grid" -> {
        requireNonEmptyArray(section.path("links"), "patient_pages.pages[].sections[].links");
        validateRouteTargets(section.path("links"), "patient_pages.pages[].sections[].links");
      }
      case "department_links" -> validateRouteTargets(section.path("links"), "patient_pages.pages[].sections[].links");
      default -> throw new BusinessException(400, "Unsupported patient page section type: " + type);
    }
  }

  private void validateRouteTargets(JsonNode links, String path) {
    for (JsonNode link : array(links, path)) {
      requireText(link, "label", path + "[].label");
      requireRouteName(link, path + "[].routeName");
    }
  }

  private void validateCards(JsonNode cards, String path) {
    requireNonEmptyArray(cards, path);
    for (JsonNode card : array(cards, path)) {
      requireText(card, "title", path + "[].title");
      requireText(card, "text", path + "[].text");
      validateOptionalAction(card.path("target"), path + "[].target");
    }
  }

  private void validateFaqItems(JsonNode items, String path) {
    requireNonEmptyArray(items, path);
    for (JsonNode item : array(items, path)) {
      requireText(item, "question", path + "[].question");
      requireText(item, "answer", path + "[].answer");
    }
  }

  private void validateTimelineItems(JsonNode items, String path) {
    requireNonEmptyArray(items, path);
    for (JsonNode item : array(items, path)) {
      requireText(item, "title", path + "[].title");
      requireText(item, "text", path + "[].text");
    }
  }

  private void requireNonEmptyArray(JsonNode node, String path) {
    if (!node.isArray() || node.isEmpty()) {
      throw new BusinessException(400, path + " must be a non-empty array");
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
    if ("cms-page".equals(routeName)) {
      requireText(node, "slug", path.replace(".routeName", ".slug"));
    }
  }

  private void requireAllowedRoute(String routeName, String path) {
    if (!ROUTES.contains(routeName)) {
      throw new BusinessException(400, "Unsupported patient routeName at " + path + ": " + routeName);
    }
  }

  private PreviewPayload parsePreviewToken(String token) {
    if (isBlank(token) || !token.contains(".")) {
      throw new BusinessException(401, "Preview token is required");
    }
    String[] parts = token.split("\\.", 2);
    if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
      throw new BusinessException(401, "Preview token is invalid");
    }
    String payloadText;
    try {
      payloadText = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(401, "Preview token is invalid");
    }
    String[] values = payloadText.split("\\|", 3);
    if (values.length != 3) {
      throw new BusinessException(401, "Preview token is invalid");
    }
    String configKey = requireConfigKey(values[0]);
    int version;
    long expiresAt;
    try {
      version = Integer.parseInt(values[1]);
      expiresAt = Long.parseLong(values[2]);
    } catch (NumberFormatException ex) {
      throw new BusinessException(401, "Preview token is invalid");
    }
    if (Instant.now().getEpochSecond() > expiresAt) {
      throw new BusinessException(401, "Preview token is expired");
    }
    return new PreviewPayload(configKey, version);
  }

  private String sign(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(previewSecret, "HmacSHA256"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new BusinessException(500, "Preview token signing failed");
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

  private record PreviewPayload(String configKey, int version) {}
}
