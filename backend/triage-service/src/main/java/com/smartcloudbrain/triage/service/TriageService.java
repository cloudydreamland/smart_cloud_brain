package com.smartcloudbrain.triage.service;

import com.smartcloudbrain.aiapi.dto.TriageRequest;
import com.smartcloudbrain.aiapi.dto.TriageResponse;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.triage.entity.TriageRecord;
import com.smartcloudbrain.triage.entity.Patient;
import com.smartcloudbrain.triage.entity.Department;
import com.smartcloudbrain.triage.repository.PatientRepository;
import com.smartcloudbrain.triage.repository.TriageRecordRepository;
import com.smartcloudbrain.triage.repository.DepartmentRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TriageService {

  private final AiGatewayService aiGatewayService;
  private final TriageRecordRepository triageRecordRepository;
  private final PatientRepository patientRepository;
  private final DepartmentRepository departmentRepository;
  private final CurrentUserService currentUserService;
  private final RedisRateLimiter redisRateLimiter;

  public TriageService(
      AiGatewayService aiGatewayService,
      TriageRecordRepository triageRecordRepository,
      PatientRepository patientRepository,
      DepartmentRepository departmentRepository,
      CurrentUserService currentUserService,
      RedisRateLimiter redisRateLimiter
  ) {
    this.aiGatewayService = aiGatewayService;
    this.triageRecordRepository = triageRecordRepository;
    this.patientRepository = patientRepository;
    this.departmentRepository = departmentRepository;
    this.currentUserService = currentUserService;
    this.redisRateLimiter = redisRateLimiter;
  }

  @Transactional
  public Map<String, Object> consult(TriageRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    if (!redisRateLimiter.allow("rate:triage:user:" + user.userId(), 10, Duration.ofMinutes(1))) {
      throw new BusinessException(429, "triage requests too frequent");
    }
    Long patientId = request.patientId() == null ? user.userId() : request.patientId();
    if (!patientId.equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Patient patient = patientRepository.findById(patientId).orElse(null);
    TriageResponse response = aiGatewayService.triage(new TriageRequest(
        patientId,
        request.chiefComplaint(),
        request.symptoms(),
        patient == null ? request.age() : patient.getAge(),
        patient == null ? request.gender() : patient.getGender(),
        patient == null ? request.allergyHistory() : patient.getAllergyHistory(),
        patient == null ? request.pastHistory() : patient.getPastHistory()
    ));
    TriageRecord record = new TriageRecord();
    record.setPatientId(patientId);
    record.setChiefComplaint(request.chiefComplaint());
    record.setRecommendedDepartment(normalizeDepartmentName(response.recommendedDepartment(), response.departmentCode()));
    record.setRecommendedDoctorIds(response.recommendedDoctorIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
    record.setReason(response.reason());
    record.setAiResultJson("""
        {"departmentCode":"%s","recommendedDoctorDirection":"%s","urgencyLevel":"%s","confidence":%s,"degraded":%s,"provider":"%s","model":"%s"}
        """.formatted(
        response.departmentCode(),
        response.recommendedDoctorDirection(),
        response.urgencyLevel(),
        response.confidence(),
        response.degraded(),
        response.provider(),
        response.model()
    ).trim());
    record.setStatus(response.degraded() ? "MANUAL_REQUIRED" : "AI_RECOMMENDED");
    return triageView(triageRecordRepository.save(record), response);
  }

  public List<Map<String, Object>> list() {
    AuthenticatedUser user = currentUserService.get();
    List<TriageRecord> records;
    if (user.role() == RoleType.PATIENT) {
      records = triageRecordRepository.findByPatientId(user.userId());
    } else if (user.role() == RoleType.DOCTOR) {
      records = triageRecordRepository.findByAssignedDoctorId(user.userId());
    } else {
      records = triageRecordRepository.findAll();
    }
    return records.stream().map(record -> triageView(record, null)).toList();
  }

  private Map<String, Object> triageView(TriageRecord record, TriageResponse response) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("triageRecordId", record.getId());
    view.put("patientId", record.getPatientId());
    view.put("chiefComplaint", record.getChiefComplaint());
    view.put("recommendedDepartment", record.getRecommendedDepartment() == null ? "" : record.getRecommendedDepartment());
    view.put("departmentCode", response == null ? "" : response.departmentCode());
    view.put("recommendedDoctorDirection", response == null ? "" : response.recommendedDoctorDirection());
    view.put("urgencyLevel", response == null ? "" : response.urgencyLevel());
    view.put("confidence", response == null || response.confidence() == null ? 0 : response.confidence());
    view.put("recommendedDoctorIds", response == null ? record.getRecommendedDoctorIds() : response.recommendedDoctorIds());
    view.put("assignedDoctorId", record.getAssignedDoctorId() == null ? 0L : record.getAssignedDoctorId());
    view.put("reason", record.getReason() == null ? "" : record.getReason());
    view.put("status", record.getStatus());
    view.put("degraded", response != null && response.degraded());
    view.put("provider", response == null ? "" : response.provider());
    view.put("model", response == null ? "" : response.model());
    return view;
  }

  private String normalizeDepartmentName(String aiName, String departmentCode) {
    if (aiName == null || aiName.isBlank()) {
      return aiName;
    }
    // 1. Try matching by departmentCode (most reliable)
    if (departmentCode != null && !departmentCode.isBlank()) {
      return departmentRepository.findByCodeIgnoreCase(departmentCode)
          .map(Department::getName)
          .orElseGet(() -> normalizeByName(aiName));
    }
    // 2. Fallback to name-based matching
    return normalizeByName(aiName);
  }

  private String normalizeByName(String aiName) {
    if (aiName == null || aiName.isBlank()) {
      return aiName;
    }
    return departmentRepository.findAllByOrderByIdAsc().stream()
        .filter(d -> aiName.contains(d.getName()) || d.getName().contains(aiName))
        .findFirst()
        .map(Department::getName)
        .orElse(aiName); // No match found — keep original, do NOT guess
  }
}


