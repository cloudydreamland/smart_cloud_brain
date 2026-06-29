package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.admin.client.InternalAiClient;
import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.client.InternalTriageClient;
import com.smartcloudbrain.admin.dto.admin.AccountSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleGenerateRequest;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.entity.AdminUser;
import com.smartcloudbrain.admin.entity.Department;
import com.smartcloudbrain.admin.entity.AiScheduleSuggestion;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.entity.Drug;
import com.smartcloudbrain.admin.repository.AdminUserRepository;
import com.smartcloudbrain.admin.repository.AiScheduleSuggestionRepository;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.DrugRepository;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.PasswordHashService;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.aiapi.dto.ExistingSchedule;
import com.smartcloudbrain.aiapi.dto.ScheduleDepartmentCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleDoctorCandidate;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestRequest;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestResponse;
import com.smartcloudbrain.aiapi.dto.ScheduleSuggestionItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCatalogService {

  private final DepartmentRepository departmentRepository;
  private final DoctorRepository doctorRepository;
  private final DrugRepository drugRepository;
  private final AiScheduleSuggestionRepository aiScheduleSuggestionRepository;
  private final AdminUserRepository adminUserRepository;
  private final InternalDoctorClient internalDoctorClient;
  private final InternalAiClient internalAiClient;
  private final InternalTriageClient internalTriageClient;
  private final PasswordHashService passwordHashService;

  public AdminCatalogService(
      DepartmentRepository departmentRepository,
      DoctorRepository doctorRepository,
      DrugRepository drugRepository,
      AiScheduleSuggestionRepository aiScheduleSuggestionRepository,
      AdminUserRepository adminUserRepository,
      InternalDoctorClient internalDoctorClient,
      InternalAiClient internalAiClient,
      InternalTriageClient internalTriageClient,
      PasswordHashService passwordHashService
  ) {
    this.departmentRepository = departmentRepository;
    this.doctorRepository = doctorRepository;
    this.drugRepository = drugRepository;
    this.aiScheduleSuggestionRepository = aiScheduleSuggestionRepository;
    this.adminUserRepository = adminUserRepository;
    this.internalDoctorClient = internalDoctorClient;
    this.internalAiClient = internalAiClient;
    this.internalTriageClient = internalTriageClient;
    this.passwordHashService = passwordHashService;
  }

  public List<Map<String, Object>> departments() {
    return departmentRepository.findAll().stream().map(this::departmentView).toList();
  }

  public List<Map<String, Object>> doctors(Long departmentId) {
    List<Doctor> doctors = departmentId == null ? doctorRepository.findAll() : doctorRepository.findByDepartmentId(departmentId);
    return doctors.stream().map(this::doctorView).toList();
  }

  public List<Map<String, Object>> accounts() {
    List<Map<String, Object>> rows = new java.util.ArrayList<>();
    adminUserRepository.findAll().stream().map(this::adminAccountView).forEach(rows::add);
    doctorRepository.findAll().stream().map(this::doctorAccountView).forEach(rows::add);
    return rows.stream()
        .sorted(java.util.Comparator
            .comparing((Map<String, Object> row) -> String.valueOf(row.get("role")))
            .thenComparing(row -> String.valueOf(row.get("account"))))
        .toList();
  }

  public List<Map<String, Object>> roles() {
    return List.of(
        roleView(RoleType.ADMIN, "系统管理员", "系统配置、基础目录、AI 配置、排班发布、分诊台、账户与权限管理"),
        roleView(RoleType.DOCTOR, "医生", "医生工作台、接诊队列、病历书写、处方审核、医生通知"),
        roleView(RoleType.PATIENT, "患者", "患者端登录、症状分诊、预约挂号、病历处方查看")
    );
  }

  @CacheEvict(cacheNames = "adminCatalog", allEntries = true)
  @Transactional
  public Map<String, Object> saveAccount(AccountSaveRequest request) {
    RoleType role = parseRole(request.role());
    if (role == RoleType.ADMIN) {
      return saveAdminAccount(request);
    }
    if (role == RoleType.DOCTOR) {
      return doctorAccountView(saveDoctorEntity(new DoctorSaveRequest(
          request.id(),
          request.name(),
          request.account(),
          request.password(),
          request.departmentId(),
          request.title(),
          request.specialty(),
          request.status()
      )));
    }
    throw new BusinessException(400, "Patient accounts are self-registered and cannot be assigned in admin console");
  }

  @CacheEvict(cacheNames = "adminCatalog", allEntries = true)
  @Transactional
  public Map<String, Object> saveDepartment(DepartmentSaveRequest request) {
    Department department = request.id() == null
        ? departmentRepository.findByCode(request.code()).orElseGet(Department::new)
        : departmentRepository.findById(request.id()).orElseGet(Department::new);
    department.setCode(request.code());
    department.setName(request.name());
    department.setDescription(request.description());
    return departmentView(departmentRepository.save(department));
  }

  @CacheEvict(cacheNames = "adminCatalog", allEntries = true)
  @Transactional
  public Map<String, Object> saveDoctor(DoctorSaveRequest request) {
    return doctorView(saveDoctorEntity(request));
  }

  private Doctor saveDoctorEntity(DoctorSaveRequest request) {
    Doctor doctor = request.id() == null ? new Doctor() : doctorRepository.findById(request.id()).orElseGet(Doctor::new);
    doctor.setName(request.name());
    doctor.setPhone(request.phone());
    if (request.password() != null && !request.password().isBlank()) {
      doctor.setPasswordHash(passwordHashService.encode(request.password()));
    } else if (doctor.getPasswordHash() == null || doctor.getPasswordHash().isBlank()) {
      doctor.setPasswordHash(passwordHashService.encode("123456"));
    }
    doctor.setDepartmentId(request.departmentId());
    doctor.setTitle(request.title());
    doctor.setSpecialty(request.specialty());
    doctor.setStatus(request.status() == null ? "ENABLED" : request.status());
    return doctorRepository.save(doctor);
  }

  @Cacheable(cacheNames = "adminCatalog", key = "'drugs'")
  public List<Map<String, Object>> drugs() {
    return drugRepository.findAll().stream().map(this::drugView).toList();
  }

  public List<Map<String, Object>> searchDrugs(String keyword) {
    String normalizedKeyword = normalize(keyword);
    if (normalizedKeyword.isBlank()) {
      return drugs();
    }
    return drugRepository.findAll().stream()
        .filter(drug -> containsKeyword(drug.getName(), normalizedKeyword)
            || containsKeyword(drug.getSpecification(), normalizedKeyword)
            || containsKeyword(drug.getContraindication(), normalizedKeyword)
            || containsKeyword(drug.getInteractionRule(), normalizedKeyword))
        .map(this::drugView)
        .toList();
  }

  @CacheEvict(cacheNames = "adminCatalog", allEntries = true)
  @Transactional
  public Map<String, Object> saveDrug(DrugSaveRequest request) {
    Drug drug = request.id() == null ? new Drug() : drugRepository.findById(request.id()).orElseGet(Drug::new);
    drug.setName(request.name());
    drug.setSpecification(request.specification());
    drug.setContraindication(request.contraindication());
    drug.setInteractionRule(request.interactionRule());
    drug.setStatus(request.status() == null ? "ENABLED" : request.status());
    drug.setUpdatedAt(LocalDateTime.now());
    Drug saved = drugRepository.save(drug);
    return drugView(saved);
  }

  public Object aiLogs() {
    return internalAiClient.recentLogs();
  }

  @Transactional
  public List<Map<String, Object>> generateScheduleSuggestions(ScheduleGenerateRequest request) {
    LocalDate startDate = request.startDate() == null ? LocalDate.now().plusDays(1) : request.startDate();
    int days = request.days() == null ? 3 : Math.max(1, Math.min(request.days(), 14));
    List<Department> departments = departmentRepository.findAll();
    Map<Long, Department> departmentById = departments.stream()
        .collect(java.util.stream.Collectors.toMap(Department::getId, department -> department));
    List<Doctor> doctors = doctorRepository.findAll().stream()
        .filter(doctor -> "ENABLED".equalsIgnoreCase(doctor.getStatus()))
        .toList();
    if (doctors.isEmpty()) {
      throw new BusinessException(600, "没有可参与排班的启用医生");
    }

    List<ExistingSchedule> existingSchedules = internalDoctorClient.schedules().stream()
        .map(this::existingSchedule)
        .filter(java.util.Objects::nonNull)
        .toList();
    ScheduleSuggestRequest aiRequest = new ScheduleSuggestRequest(
        startDate,
        days,
        doctors.stream().map(doctor -> {
          Department department = departmentById.get(doctor.getDepartmentId());
          return new ScheduleDoctorCandidate(
              doctor.getId(), doctor.getName(), doctor.getDepartmentId(),
              department == null ? "" : department.getCode(), doctor.getSpecialty(), true);
        }).toList(),
        departments.stream().map(department -> new ScheduleDepartmentCandidate(
            department.getId(), department.getCode(), department.getName())).toList(),
        existingSchedules
    );
    ScheduleSuggestResponse aiResponse = internalAiClient.suggestSchedule(aiRequest);
    List<ScheduleSuggestionItem> validated = validateSuggestions(
        aiResponse.suggestions(), doctors, startDate, days, existingSchedules);

    aiScheduleSuggestionRepository.deleteAll(
        aiScheduleSuggestionRepository.findByStatusOrderByWorkDateAscDoctorIdAsc("DRAFT"));
    for (ScheduleSuggestionItem item : validated) {
      AiScheduleSuggestion suggestion = new AiScheduleSuggestion();
      suggestion.setDoctorId(item.doctorId());
      suggestion.setDepartmentId(item.departmentId());
      suggestion.setWorkDate(item.workDate());
      suggestion.setTimeRange(item.timeRange());
      suggestion.setCapacity(item.capacity());
      suggestion.setReason(item.reason());
      suggestion.setStatus("DRAFT");
      suggestion.setSource(aiResponse.provider());
      suggestion.setDegraded(aiResponse.degraded());
      suggestion.setUpdatedAt(LocalDateTime.now());
      aiScheduleSuggestionRepository.save(suggestion);
    }
    return aiScheduleSuggestionRepository.findByStatusOrderByWorkDateAscDoctorIdAsc("DRAFT").stream()
        .map(this::scheduleSuggestionView)
        .toList();
  }

  private List<ScheduleSuggestionItem> validateSuggestions(
      List<ScheduleSuggestionItem> suggestions,
      List<Doctor> enabledDoctors,
      LocalDate startDate,
      int days,
      List<ExistingSchedule> existingSchedules
  ) {
    if (suggestions == null || suggestions.isEmpty()) {
      throw new BusinessException(600, "AI 未返回任何排班建议");
    }
    Map<Long, Doctor> doctorById = enabledDoctors.stream()
        .collect(java.util.stream.Collectors.toMap(Doctor::getId, doctor -> doctor));
    HashSet<String> occupied = new HashSet<>();
    existingSchedules.forEach(item -> occupied.add(scheduleKey(item.doctorId(), item.workDate(), item.timeRange())));
    List<ScheduleSuggestionItem> validated = new java.util.ArrayList<>();
    LocalDate endDate = startDate.plusDays(days);
    for (ScheduleSuggestionItem item : suggestions) {
      Doctor doctor = item == null ? null : doctorById.get(item.doctorId());
      if (doctor == null || !doctor.getDepartmentId().equals(item.departmentId())) {
        throw new BusinessException(600, "AI 排班包含无效医生或科室不匹配");
      }
      if (item.workDate() == null || item.workDate().isBefore(startDate) || !item.workDate().isBefore(endDate)) {
        throw new BusinessException(600, "AI 排班日期超出请求范围");
      }
      validateTimeRange(item.timeRange());
      if (item.capacity() == null || item.capacity() < 1 || item.capacity() > 100) {
        throw new BusinessException(600, "AI 排班容量必须在 1-100 之间");
      }
      String key = scheduleKey(item.doctorId(), item.workDate(), item.timeRange());
      if (!occupied.add(key)) {
        throw new BusinessException(600, "AI 排班包含重复或已存在的医生时段");
      }
      validated.add(item);
    }
    return validated;
  }

  private void validateTimeRange(String timeRange) {
    if (timeRange == null || !timeRange.matches("^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$")) {
      throw new BusinessException(600, "AI 排班时段格式必须为 HH:mm-HH:mm");
    }
    String[] parts = timeRange.split("-");
    if (!LocalTime.parse(parts[0]).isBefore(LocalTime.parse(parts[1]))) {
      throw new BusinessException(600, "AI 排班结束时间必须晚于开始时间");
    }
  }

  private ExistingSchedule existingSchedule(Map<String, Object> schedule) {
    try {
      Long doctorId = numberValue(schedule.get("doctorId"));
      Object dateValue = schedule.get("workDate");
      Object rangeValue = schedule.get("timeRange");
      if (doctorId == null || dateValue == null || rangeValue == null) {
        return null;
      }
      return new ExistingSchedule(doctorId, LocalDate.parse(String.valueOf(dateValue)), String.valueOf(rangeValue));
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private String scheduleKey(Long doctorId, LocalDate workDate, String timeRange) {
    return doctorId + "|" + workDate + "|" + timeRange;
  }

  @Transactional
  public List<Map<String, Object>> publishSchedule(SchedulePublishRequest request) {
    List<AiScheduleSuggestion> suggestions = request.suggestionIds() == null || request.suggestionIds().isEmpty()
        ? aiScheduleSuggestionRepository.findByStatusOrderByWorkDateAscDoctorIdAsc("DRAFT")
        : aiScheduleSuggestionRepository.findAllById(request.suggestionIds()).stream()
            .filter(suggestion -> "DRAFT".equalsIgnoreCase(suggestion.getStatus()))
            .toList();
    List<Map<String, Object>> schedulesToPublish = suggestions.stream()
        .map(suggestion -> Map.<String, Object>of(
            "doctorId", suggestion.getDoctorId(),
            "departmentId", suggestion.getDepartmentId(),
            "workDate", suggestion.getWorkDate().toString(),
            "timeRange", suggestion.getTimeRange(),
            "capacity", suggestion.getCapacity()
        ))
        .toList();
    List<Map<String, Object>> publishedSchedules = internalDoctorClient.publishSchedules(schedulesToPublish);
    for (AiScheduleSuggestion suggestion : suggestions) {
      suggestion.setStatus("PUBLISHED");
      suggestion.setUpdatedAt(LocalDateTime.now());
      aiScheduleSuggestionRepository.save(suggestion);
    }
    return publishedSchedules;
  }

  public List<Map<String, Object>> schedules() {
    return internalDoctorClient.schedules();
  }

  public Map<String, Object> scheduleSuggestionDetail(Long id) {
    return scheduleSuggestionView(aiScheduleSuggestionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  public List<Map<String, Object>> triageDesk() {
    return internalTriageClient.list().stream().map(this::enrichTriageView).toList();
  }

  public Map<String, Object> triageDetail(Long id) {
    return enrichTriageView(internalTriageClient.detail(id));
  }

  public Map<String, Object> assignTriage(Long triageRecordId, Long doctorId) {
    doctorRepository.findById(doctorId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return enrichTriageView(internalTriageClient.assign(triageRecordId, doctorId));
  }

  public Map<String, Object> closeTriage(Long triageRecordId) {
    return enrichTriageView(internalTriageClient.close(triageRecordId));
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private boolean containsKeyword(String value, String keyword) {
    return normalize(value).contains(keyword);
  }

  private Map<String, Object> departmentView(Department department) {
    return Map.of(
        "id", department.getId(),
        "code", department.getCode(),
        "name", department.getName(),
        "description", department.getDescription() == null ? "" : department.getDescription()
    );
  }

  private Map<String, Object> doctorView(Doctor doctor) {
    return Map.of(
        "id", doctor.getId(),
        "name", doctor.getName(),
        "phone", doctor.getPhone() == null ? "" : doctor.getPhone(),
        "departmentId", doctor.getDepartmentId(),
        "title", doctor.getTitle() == null ? "" : doctor.getTitle(),
        "specialty", doctor.getSpecialty() == null ? "" : doctor.getSpecialty(),
        "status", doctor.getStatus() == null ? "ENABLED" : doctor.getStatus()
    );
  }

  private Map<String, Object> drugView(Drug drug) {
    return Map.of(
        "id", drug.getId(),
        "name", drug.getName(),
        "specification", drug.getSpecification() == null ? "" : drug.getSpecification(),
        "contraindication", drug.getContraindication() == null ? "" : drug.getContraindication(),
        "interactionRule", drug.getInteractionRule() == null ? "" : drug.getInteractionRule(),
        "status", drug.getStatus() == null ? "ENABLED" : drug.getStatus()
    );
  }

  private Map<String, Object> saveAdminAccount(AccountSaveRequest request) {
    AdminUser admin = request.id() == null
        ? adminUserRepository.findByUsername(request.account()).orElseGet(AdminUser::new)
        : adminUserRepository.findById(request.id()).orElseGet(AdminUser::new);
    String nextStatus = request.status() == null || request.status().isBlank() ? "ENABLED" : request.status();
    if ("DISABLED".equalsIgnoreCase(nextStatus)
        && admin.getId() != null
        && "ENABLED".equalsIgnoreCase(admin.getStatus())
        && adminUserRepository.countByStatus("ENABLED") <= 1) {
      throw new BusinessException(400, "At least one administrator account must remain enabled");
    }
    if (admin.getId() == null && (request.password() == null || request.password().isBlank())) {
      throw new BusinessException(400, "Password is required for new administrator accounts");
    }
    admin.setUsername(request.account());
    admin.setName(request.name());
    admin.setStatus(nextStatus);
    if (request.password() != null && !request.password().isBlank()) {
      admin.setPasswordHash(passwordHashService.encode(request.password()));
    }
    admin.setUpdatedAt(LocalDateTime.now());
    return adminAccountView(adminUserRepository.save(admin));
  }

  private RoleType parseRole(String role) {
    try {
      return RoleType.valueOf(normalize(role).toUpperCase(Locale.ROOT));
    } catch (Exception ex) {
      throw new BusinessException(400, "Unsupported role: " + role);
    }
  }

  private Map<String, Object> roleView(RoleType role, String label, String permissions) {
    return Map.of(
        "role", role.name(),
        "label", label,
        "permissions", permissions
    );
  }

  private Map<String, Object> adminAccountView(AdminUser admin) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", admin.getId());
    view.put("role", RoleType.ADMIN.name());
    view.put("roleLabel", "系统管理员");
    view.put("account", admin.getUsername());
    view.put("name", admin.getName());
    view.put("departmentId", 0L);
    view.put("departmentName", "");
    view.put("title", "");
    view.put("specialty", "");
    view.put("status", admin.getStatus() == null ? "ENABLED" : admin.getStatus());
    view.put("permissions", "系统配置、基础目录、AI 配置、排班发布、分诊台、账户与权限管理");
    return view;
  }

  private Map<String, Object> doctorAccountView(Doctor doctor) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", doctor.getId());
    view.put("role", RoleType.DOCTOR.name());
    view.put("roleLabel", "医生");
    view.put("account", doctor.getPhone() == null ? "" : doctor.getPhone());
    view.put("name", doctor.getName());
    view.put("departmentId", doctor.getDepartmentId() == null ? 0L : doctor.getDepartmentId());
    view.put("departmentName", departmentName(doctor.getDepartmentId()));
    view.put("title", doctor.getTitle() == null ? "" : doctor.getTitle());
    view.put("specialty", doctor.getSpecialty() == null ? "" : doctor.getSpecialty());
    view.put("status", doctor.getStatus() == null ? "ENABLED" : doctor.getStatus());
    view.put("permissions", "医生工作台、接诊队列、病历书写、处方审核、医生通知");
    return view;
  }

  private Map<String, Object> scheduleSuggestionView(AiScheduleSuggestion suggestion) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", suggestion.getId());
    view.put("doctorId", suggestion.getDoctorId());
    view.put("doctorName", doctorName(suggestion.getDoctorId()));
    view.put("departmentId", suggestion.getDepartmentId());
    view.put("departmentName", departmentName(suggestion.getDepartmentId()));
    view.put("workDate", suggestion.getWorkDate().toString());
    view.put("timeRange", suggestion.getTimeRange());
    view.put("capacity", suggestion.getCapacity());
    view.put("reason", suggestion.getReason() == null ? "" : suggestion.getReason());
    view.put("status", suggestion.getStatus());
    view.put("source", suggestion.getSource() == null ? "unknown" : suggestion.getSource());
    view.put("degraded", Boolean.TRUE.equals(suggestion.getDegraded()));
    return view;
  }

  private Map<String, Object> enrichTriageView(Map<String, Object> record) {
    Map<String, Object> view = new LinkedHashMap<>(record);
    view.put("assignedDoctorName", doctorName(numberValue(record.get("assignedDoctorId"))));
    return view;
  }

  private String doctorName(Long doctorId) {
    if (doctorId == null) {
      return "";
    }
    return doctorRepository.findById(doctorId).map(Doctor::getName).orElse("");
  }

  private String departmentName(Long departmentId) {
    if (departmentId == null) {
      return "";
    }
    return departmentRepository.findById(departmentId).map(Department::getName).orElse("");
  }

  private Long numberValue(Object value) {
    if (value instanceof Number number) {
      long longValue = number.longValue();
      return longValue == 0L ? null : longValue;
    }
    return null;
  }
}
