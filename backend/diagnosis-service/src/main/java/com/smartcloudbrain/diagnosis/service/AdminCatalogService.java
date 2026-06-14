package com.smartcloudbrain.diagnosis.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.diagnosis.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.KnowledgeEntrySaveRequest;
import com.smartcloudbrain.diagnosis.dto.admin.PromptTemplateSaveRequest;
import com.smartcloudbrain.diagnosis.entity.Department;
import com.smartcloudbrain.diagnosis.entity.Doctor;
import com.smartcloudbrain.diagnosis.entity.Drug;
import com.smartcloudbrain.diagnosis.entity.KnowledgeEntry;
import com.smartcloudbrain.diagnosis.entity.PromptTemplate;
import com.smartcloudbrain.diagnosis.repository.DepartmentRepository;
import com.smartcloudbrain.diagnosis.repository.DoctorRepository;
import com.smartcloudbrain.diagnosis.repository.DrugRepository;
import com.smartcloudbrain.diagnosis.repository.KnowledgeEntryRepository;
import com.smartcloudbrain.diagnosis.repository.PromptTemplateRepository;
import java.time.LocalDateTime;
import java.util.List;
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

  public AdminCatalogService(
      DepartmentRepository departmentRepository,
      DoctorRepository doctorRepository,
      DrugRepository drugRepository,
      PromptTemplateRepository promptTemplateRepository,
      KnowledgeEntryRepository knowledgeEntryRepository
  ) {
    this.departmentRepository = departmentRepository;
    this.doctorRepository = doctorRepository;
    this.drugRepository = drugRepository;
    this.promptTemplateRepository = promptTemplateRepository;
    this.knowledgeEntryRepository = knowledgeEntryRepository;
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
    doctor.setPasswordHash(request.password() == null || request.password().isBlank() ? "$2a$mock" : "{plain}" + request.password());
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
    return drugView(drugRepository.save(drug));
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
    return promptView(promptTemplateRepository.save(prompt));
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
    return knowledgeView(knowledgeEntryRepository.save(entry));
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
}
