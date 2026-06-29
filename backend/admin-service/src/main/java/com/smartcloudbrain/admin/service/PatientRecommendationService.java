package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSortRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationStatusRequest;
import com.smartcloudbrain.admin.entity.Department;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.entity.PatientRecommendation;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.PatientRecommendationRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientRecommendationService {

  private static final String TYPE_DEPARTMENT = "DEPARTMENT";
  private static final String TYPE_DOCTOR = "DOCTOR";
  private static final String STATUS_ENABLED = "ENABLED";
  private static final String STATUS_DISABLED = "DISABLED";

  private final PatientRecommendationRepository repository;
  private final DepartmentRepository departmentRepository;
  private final DoctorRepository doctorRepository;

  public PatientRecommendationService(
      PatientRecommendationRepository repository,
      DepartmentRepository departmentRepository,
      DoctorRepository doctorRepository
  ) {
    this.repository = repository;
    this.departmentRepository = departmentRepository;
    this.doctorRepository = doctorRepository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminList(String type) {
    String recommendType = normalizeType(type);
    return repository.findByRecommendTypeAndDeletedFalseOrderBySortAscUpdatedAtDesc(recommendType).stream()
        .map(row -> view(row, false))
        .toList();
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "patientSite", key = "'recommendations:' + #type")
  public List<Map<String, Object>> publicList(String type) {
    String recommendType = normalizeType(type);
    return repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc(recommendType, STATUS_ENABLED).stream()
        .map(row -> view(row, true))
        .filter(row -> Boolean.TRUE.equals(row.get("targetAvailable")))
        .toList();
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public Map<String, Object> save(PatientRecommendationSaveRequest request, Long userId) {
    String recommendType = normalizeType(request.recommendType());
    ensureTarget(recommendType, request.targetId(), false);
    PatientRecommendation recommendation = request.id() == null
        ? new PatientRecommendation()
        : repository.findById(request.id()).filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
            .orElseThrow(() -> new BusinessException(404, "Recommendation not found"));
    recommendation.setRecommendType(recommendType);
    recommendation.setTargetId(request.targetId());
    recommendation.setTitle(blankToNull(request.title()));
    recommendation.setDescription(blankToNull(request.description()));
    recommendation.setImageUrl(blankToNull(request.imageUrl()));
    recommendation.setImageObjectKey(blankToNull(request.imageObjectKey()));
    recommendation.setSort(request.sort() == null ? 0 : request.sort());
    recommendation.setStatus(normalizeStatus(request.status()));
    recommendation.setDeleted(false);
    recommendation.setUpdatedBy(userId);
    recommendation.setUpdatedAt(LocalDateTime.now());
    if (recommendation.getId() == null) {
      recommendation.setCreatedBy(userId);
    }
    return view(repository.save(recommendation), false);
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public Map<String, Object> updateStatus(PatientRecommendationStatusRequest request, Long userId) {
    PatientRecommendation recommendation = activeRecommendation(request.id());
    recommendation.setStatus(normalizeStatus(request.status()));
    recommendation.setUpdatedBy(userId);
    recommendation.setUpdatedAt(LocalDateTime.now());
    return view(repository.save(recommendation), false);
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public Map<String, Object> delete(Long id, Long userId) {
    PatientRecommendation recommendation = activeRecommendation(id);
    recommendation.setDeleted(true);
    recommendation.setUpdatedBy(userId);
    recommendation.setUpdatedAt(LocalDateTime.now());
    return view(repository.save(recommendation), false);
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public List<Map<String, Object>> sort(PatientRecommendationSortRequest request, Long userId) {
    if (request.items().isEmpty()) {
      throw new BusinessException(400, "排序列表不能为空");
    }
    String recommendType = activeRecommendation(request.items().get(0).id()).getRecommendType();
    for (PatientRecommendationSortRequest.SortItem item : request.items()) {
      PatientRecommendation recommendation = activeRecommendation(item.id());
      if (!recommendType.equals(recommendation.getRecommendType())) {
        throw new BusinessException(400, "不能跨推荐类型排序");
      }
      recommendation.setSort(item.sort());
      recommendation.setUpdatedBy(userId);
      recommendation.setUpdatedAt(LocalDateTime.now());
      repository.save(recommendation);
    }
    return adminList(recommendType);
  }

  private PatientRecommendation activeRecommendation(Long id) {
    return repository.findById(id)
        .filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
        .orElseThrow(() -> new BusinessException(404, "Recommendation not found"));
  }

  private Map<String, Object> view(PatientRecommendation recommendation, boolean publicOnly) {
    Map<String, Object> target = ensureTarget(recommendation.getRecommendType(), recommendation.getTargetId(), publicOnly);
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", recommendation.getId());
    row.put("recommendType", recommendation.getRecommendType());
    row.put("targetId", recommendation.getTargetId());
    row.put("title", safe(recommendation.getTitle(), safe((String) target.get("name"))));
    row.put("description", safe(recommendation.getDescription(), safe((String) target.get("description"))));
    row.put("imageUrl", safe(recommendation.getImageUrl()));
    row.put("imageObjectKey", safe(recommendation.getImageObjectKey()));
    row.put("sort", recommendation.getSort() == null ? 0 : recommendation.getSort());
    row.put("status", safe(recommendation.getStatus(), STATUS_ENABLED));
    row.put("targetAvailable", Boolean.TRUE.equals(target.get("available")));
    row.put("targetName", safe((String) target.get("name")));
    row.put("departmentId", target.get("departmentId"));
    row.put("departmentName", safe((String) target.get("departmentName")));
    row.put("doctorTitle", safe((String) target.get("doctorTitle")));
    row.put("specialty", safe((String) target.get("specialty")));
    row.put("createdBy", recommendation.getCreatedBy());
    row.put("updatedBy", recommendation.getUpdatedBy());
    row.put("createdAt", recommendation.getCreatedAt() == null ? "" : recommendation.getCreatedAt().toString());
    row.put("updatedAt", recommendation.getUpdatedAt() == null ? "" : recommendation.getUpdatedAt().toString());
    return row;
  }

  private Map<String, Object> ensureTarget(String type, Long targetId, boolean requireEnabledDoctor) {
    if (targetId == null) {
      throw new BusinessException(400, "Recommendation targetId is required");
    }
    if (TYPE_DEPARTMENT.equals(type)) {
      Department department = departmentRepository.findById(targetId).orElse(null);
      if (department == null) {
        if (requireEnabledDoctor) return Map.of("available", false);
        throw new BusinessException(400, "推荐科室不存在");
      }
      Map<String, Object> target = new LinkedHashMap<>();
      target.put("available", true);
      target.put("name", department.getName());
      target.put("description", department.getDescription());
      target.put("departmentId", department.getId());
      target.put("departmentName", department.getName());
      return target;
    }
    Doctor doctor = doctorRepository.findById(targetId).orElse(null);
    if (doctor == null || (requireEnabledDoctor && !"ENABLED".equalsIgnoreCase(safe(doctor.getStatus(), STATUS_ENABLED)))) {
      if (requireEnabledDoctor) return Map.of("available", false);
      throw new BusinessException(400, "推荐医生不存在或未启用");
    }
    Department department = doctor.getDepartmentId() == null ? null : departmentRepository.findById(doctor.getDepartmentId()).orElse(null);
    Map<String, Object> target = new LinkedHashMap<>();
    target.put("available", true);
    target.put("name", doctor.getName());
    target.put("description", doctor.getSpecialty());
    target.put("departmentId", doctor.getDepartmentId());
    target.put("departmentName", department == null ? "" : department.getName());
    target.put("doctorTitle", doctor.getTitle());
    target.put("specialty", doctor.getSpecialty());
    return target;
  }

  private String normalizeType(String type) {
    String value = safe(type).toUpperCase();
    if (!TYPE_DEPARTMENT.equals(value) && !TYPE_DOCTOR.equals(value)) {
      throw new BusinessException(400, "Unsupported recommendation type: " + type);
    }
    return value;
  }

  private String normalizeStatus(String status) {
    String value = safe(status, STATUS_ENABLED).toUpperCase();
    if (!STATUS_ENABLED.equals(value) && !STATUS_DISABLED.equals(value)) {
      throw new BusinessException(400, "Unsupported recommendation status: " + status);
    }
    return value;
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String safe(String value) {
    return safe(value, "");
  }

  private String safe(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
