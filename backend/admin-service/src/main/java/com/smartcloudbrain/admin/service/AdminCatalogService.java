package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.client.InternalTriageClient;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.KnowledgeEntrySaveRequest;
import com.smartcloudbrain.admin.dto.admin.PromptTemplateSaveRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleGenerateRequest;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.dto.admin.SystemDictSaveRequest;
import com.smartcloudbrain.admin.entity.Department;
import com.smartcloudbrain.admin.entity.AiScheduleSuggestion;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.entity.Drug;
import com.smartcloudbrain.admin.entity.KnowledgeEntry;
import com.smartcloudbrain.admin.entity.PromptTemplate;
import com.smartcloudbrain.admin.entity.SystemDict;
import com.smartcloudbrain.admin.repository.AiScheduleSuggestionRepository;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.DrugRepository;
import com.smartcloudbrain.admin.repository.KnowledgeEntryRepository;
import com.smartcloudbrain.admin.repository.PromptTemplateRepository;
import com.smartcloudbrain.admin.repository.SystemDictRepository;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCatalogService {

  private final DepartmentRepository departmentRepository;
  private final DoctorRepository doctorRepository;
  private final DrugRepository drugRepository;
  private final PromptTemplateRepository promptTemplateRepository;
  private final KnowledgeEntryRepository knowledgeEntryRepository;
  private final SystemDictRepository systemDictRepository;
  private final AiScheduleSuggestionRepository aiScheduleSuggestionRepository;
  private final InternalDoctorClient internalDoctorClient;
  private final InternalTriageClient internalTriageClient;

  public AdminCatalogService(
      DepartmentRepository departmentRepository,
      DoctorRepository doctorRepository,
      DrugRepository drugRepository,
      PromptTemplateRepository promptTemplateRepository,
      KnowledgeEntryRepository knowledgeEntryRepository,
      SystemDictRepository systemDictRepository,
      AiScheduleSuggestionRepository aiScheduleSuggestionRepository,
      InternalDoctorClient internalDoctorClient,
      InternalTriageClient internalTriageClient
  ) {
    this.departmentRepository = departmentRepository;
    this.doctorRepository = doctorRepository;
    this.drugRepository = drugRepository;
    this.promptTemplateRepository = promptTemplateRepository;
    this.knowledgeEntryRepository = knowledgeEntryRepository;
    this.systemDictRepository = systemDictRepository;
    this.aiScheduleSuggestionRepository = aiScheduleSuggestionRepository;
    this.internalDoctorClient = internalDoctorClient;
    this.internalTriageClient = internalTriageClient;
  }

  public List<Map<String, Object>> departments() {
    return departmentRepository.findAll().stream().map(this::departmentView).toList();
  }

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

  @Transactional
  public Map<String, Object> saveDoctor(DoctorSaveRequest request) {
    Doctor doctor = request.id() == null ? new Doctor() : doctorRepository.findById(request.id()).orElseGet(Doctor::new);
    doctor.setName(request.name());
    doctor.setPhone(request.phone());
    if (request.password() != null && !request.password().isBlank()) {
      doctor.setPasswordHash("{plain}" + request.password());
    } else if (doctor.getPasswordHash() == null || doctor.getPasswordHash().isBlank()) {
      doctor.setPasswordHash("{plain}123456");
    }
    doctor.setDepartmentId(request.departmentId());
    doctor.setTitle(request.title());
    doctor.setSpecialty(request.specialty());
    doctor.setStatus(request.status() == null ? "ENABLED" : request.status());
    return doctorView(doctorRepository.save(doctor));
  }

  public List<Map<String, Object>> drugs() {
    return drugRepository.findAll().stream().map(this::drugView).toList();
  }

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

  public List<Map<String, Object>> prompts() {
    return promptTemplateRepository.findAll().stream().map(this::promptView).toList();
  }

  @Transactional
  public Map<String, Object> savePrompt(PromptTemplateSaveRequest request) {
    PromptTemplate prompt = request.id() == null ? new PromptTemplate() : promptTemplateRepository.findById(request.id()).orElseGet(PromptTemplate::new);
    prompt.setTaskType(request.taskType());
    prompt.setDepartmentCode(request.departmentCode());
    prompt.setTemplateName(request.templateName());
    prompt.setTemplateContent(request.templateContent());
    prompt.setOutputSchema(request.outputSchema() == null ? "{\"type\":\"object\"}" : request.outputSchema());
    prompt.setVersion(request.version() == null ? "v1" : request.version());
    prompt.setEnabled(request.enabled() == null || request.enabled());
    prompt.setUpdatedAt(LocalDateTime.now());
    PromptTemplate saved = promptTemplateRepository.save(prompt);
    return promptView(saved);
  }

  public List<Map<String, Object>> knowledgeEntries() {
    return knowledgeEntryRepository.findAll().stream().map(this::knowledgeView).toList();
  }

  @Transactional
  public Map<String, Object> saveKnowledgeEntry(KnowledgeEntrySaveRequest request) {
    KnowledgeEntry entry = request.id() == null ? new KnowledgeEntry() : knowledgeEntryRepository.findById(request.id()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    entry.setTitle(request.title());
    entry.setSymptoms(request.symptoms());
    entry.setRiskSignals(request.riskSignals());
    entry.setAdvice(request.advice());
    entry.setDepartmentCode(request.departmentCode());
    entry.setStatus(request.status() == null ? "ENABLED" : request.status());
    entry.setUpdatedAt(LocalDateTime.now());
    KnowledgeEntry saved = knowledgeEntryRepository.save(entry);
    return knowledgeView(saved);
  }

  public List<Map<String, Object>> searchKnowledge(String q, String departmentCode) {
    String query = normalize(q);
    String department = normalize(departmentCode);
    return knowledgeEntryRepository.findByStatus("ENABLED").stream()
        .filter(entry -> department.isBlank() || normalize(entry.getDepartmentCode()).equals(department))
        .filter(entry -> query.isBlank()
            || contains(entry.getTitle(), query)
            || contains(entry.getSymptoms(), query)
            || contains(entry.getRiskSignals(), query)
            || contains(entry.getAdvice(), query))
        .map(this::knowledgeView)
        .toList();
  }

  public List<Map<String, Object>> searchDrugs(String q) {
    String query = normalize(q);
    return drugRepository.findAll().stream()
        .filter(drug -> "ENABLED".equalsIgnoreCase(drug.getStatus()))
        .filter(drug -> query.isBlank()
            || contains(drug.getName(), query)
            || contains(drug.getSpecification(), query)
            || contains(drug.getContraindication(), query)
            || contains(drug.getInteractionRule(), query))
        .map(this::drugView)
        .toList();
  }

  public List<Map<String, Object>> searchPrompts(String q) {
    String query = normalize(q);
    return promptTemplateRepository.findAll().stream()
        .filter(prompt -> query.isBlank()
            || contains(prompt.getTaskType(), query)
            || contains(prompt.getDepartmentCode(), query)
            || contains(prompt.getTemplateName(), query)
            || contains(prompt.getTemplateContent(), query)
            || contains(prompt.getVersion(), query))
        .map(this::promptView)
        .toList();
  }

  public List<Map<String, Object>> dicts(String dictType) {
    List<SystemDict> dicts = normalize(dictType).isBlank()
        ? systemDictRepository.findAll()
        : systemDictRepository.findByDictTypeOrderBySortAscIdAsc(dictType);
    return dicts.stream().map(this::dictView).toList();
  }

  @Transactional
  public Map<String, Object> saveDict(SystemDictSaveRequest request) {
    SystemDict dict = request.id() == null
        ? new SystemDict()
        : systemDictRepository.findById(request.id()).orElseGet(SystemDict::new);
    dict.setDictType(request.dictType());
    dict.setDictKey(request.dictKey());
    dict.setDictValue(request.dictValue());
    dict.setSort(request.sort() == null ? 0 : request.sort());
    dict.setStatus(request.status() == null ? "ENABLED" : request.status());
    dict.setUpdatedAt(LocalDateTime.now());
    return dictView(systemDictRepository.save(dict));
  }

  @Transactional
  public List<Map<String, Object>> generateScheduleSuggestions(ScheduleGenerateRequest request) {
    LocalDate startDate = request.startDate() == null ? LocalDate.now().plusDays(1) : request.startDate();
    int days = request.days() == null ? 3 : Math.max(1, Math.min(request.days(), 14));
    List<Doctor> doctors = doctorRepository.findAll().stream()
        .filter(doctor -> "ENABLED".equalsIgnoreCase(doctor.getStatus()))
        .toList();
    for (Doctor doctor : doctors) {
      for (int offset = 0; offset < days; offset++) {
        LocalDate date = startDate.plusDays(offset);
        AiScheduleSuggestion suggestion = new AiScheduleSuggestion();
        suggestion.setDoctorId(doctor.getId());
        suggestion.setDepartmentId(doctor.getDepartmentId());
        suggestion.setWorkDate(date);
        suggestion.setTimeRange(offset % 2 == 0 ? "09:00-12:00" : "14:00-17:00");
        suggestion.setCapacity(12);
        suggestion.setReason("基于演示门诊量和医生科室自动生成");
        suggestion.setStatus("DRAFT");
        suggestion.setUpdatedAt(LocalDateTime.now());
        aiScheduleSuggestionRepository.save(suggestion);
      }
    }
    return aiScheduleSuggestionRepository.findByStatusOrderByWorkDateAscDoctorIdAsc("DRAFT").stream()
        .map(this::scheduleSuggestionView)
        .toList();
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

  private boolean contains(String value, String query) {
    return normalize(value).contains(query);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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

  private Map<String, Object> promptView(PromptTemplate prompt) {
    return Map.of(
        "id", prompt.getId(),
        "taskType", prompt.getTaskType(),
        "departmentCode", prompt.getDepartmentCode() == null ? "" : prompt.getDepartmentCode(),
        "templateName", prompt.getTemplateName(),
        "templateContent", prompt.getTemplateContent(),
        "outputSchema", prompt.getOutputSchema() == null ? "" : prompt.getOutputSchema(),
        "version", prompt.getVersion() == null ? "v1" : prompt.getVersion(),
        "enabled", Boolean.TRUE.equals(prompt.getEnabled())
    );
  }

  private Map<String, Object> knowledgeView(KnowledgeEntry entry) {
    return Map.of(
        "id", entry.getId(),
        "title", entry.getTitle(),
        "symptoms", entry.getSymptoms(),
        "riskSignals", entry.getRiskSignals() == null ? "" : entry.getRiskSignals(),
        "advice", entry.getAdvice(),
        "departmentCode", entry.getDepartmentCode() == null ? "" : entry.getDepartmentCode(),
        "status", entry.getStatus() == null ? "ENABLED" : entry.getStatus()
    );
  }

  private Map<String, Object> dictView(SystemDict dict) {
    return Map.of(
        "id", dict.getId(),
        "dictType", dict.getDictType(),
        "dictKey", dict.getDictKey(),
        "dictValue", dict.getDictValue(),
        "sort", dict.getSort() == null ? 0 : dict.getSort(),
        "status", dict.getStatus() == null ? "ENABLED" : dict.getStatus()
    );
  }

  private Map<String, Object> scheduleSuggestionView(AiScheduleSuggestion suggestion) {
    return Map.of(
        "id", suggestion.getId(),
        "doctorId", suggestion.getDoctorId(),
        "doctorName", doctorName(suggestion.getDoctorId()),
        "departmentId", suggestion.getDepartmentId(),
        "departmentName", departmentName(suggestion.getDepartmentId()),
        "workDate", suggestion.getWorkDate().toString(),
        "timeRange", suggestion.getTimeRange(),
        "capacity", suggestion.getCapacity(),
        "reason", suggestion.getReason() == null ? "" : suggestion.getReason(),
        "status", suggestion.getStatus()
    );
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


