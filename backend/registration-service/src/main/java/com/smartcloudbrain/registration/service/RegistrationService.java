package com.smartcloudbrain.registration.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.common.redis.RedisIdempotencyGuard;
import com.smartcloudbrain.common.redis.RedisRateLimiter;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.registration.dto.registration.CreateRegistrationRequest;
import com.smartcloudbrain.registration.entity.AppointmentSlot;
import com.smartcloudbrain.registration.entity.Department;
import com.smartcloudbrain.registration.entity.Doctor;
import com.smartcloudbrain.registration.entity.Patient;
import com.smartcloudbrain.registration.entity.PatientVisitor;
import com.smartcloudbrain.registration.entity.Registration;
import com.smartcloudbrain.registration.repository.AppointmentSlotRepository;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import com.smartcloudbrain.registration.repository.PatientRepository;
import com.smartcloudbrain.registration.repository.PatientVisitorRepository;
import com.smartcloudbrain.registration.repository.RegistrationRepository;
import com.smartcloudbrain.registration.event.DomainEventPublisher;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

  private final RegistrationRepository registrationRepository;
  private final DoctorRepository doctorRepository;
  private final PatientRepository patientRepository;
  private final PatientVisitorRepository patientVisitorRepository;
  private final DepartmentRepository departmentRepository;
  private final AppointmentSlotRepository appointmentSlotRepository;
  private final CurrentUserService currentUserService;
  private final RedisRateLimiter redisRateLimiter;
  private final RedisIdempotencyGuard redisIdempotencyGuard;
  private final RegistrationSlotQueryService registrationSlotQueryService;
  private final DomainEventPublisher domainEventPublisher;

  public RegistrationService(
      RegistrationRepository registrationRepository,
      DoctorRepository doctorRepository,
      PatientRepository patientRepository,
      PatientVisitorRepository patientVisitorRepository,
      DepartmentRepository departmentRepository,
      AppointmentSlotRepository appointmentSlotRepository,
      CurrentUserService currentUserService,
      RedisRateLimiter redisRateLimiter,
      RedisIdempotencyGuard redisIdempotencyGuard,
      RegistrationSlotQueryService registrationSlotQueryService,
      DomainEventPublisher domainEventPublisher
  ) {
    this.registrationRepository = registrationRepository;
    this.doctorRepository = doctorRepository;
    this.patientRepository = patientRepository;
    this.patientVisitorRepository = patientVisitorRepository;
    this.departmentRepository = departmentRepository;
    this.appointmentSlotRepository = appointmentSlotRepository;
    this.currentUserService = currentUserService;
    this.redisRateLimiter = redisRateLimiter;
    this.redisIdempotencyGuard = redisIdempotencyGuard;
    this.registrationSlotQueryService = registrationSlotQueryService;
    this.domainEventPublisher = domainEventPublisher;
  }

  @CacheEvict(cacheNames = "registration:slots", allEntries = true)
  @Transactional
  public Map<String, Object> create(CreateRegistrationRequest request) {
    return create(request, null);
  }

  @CacheEvict(cacheNames = "registration:slots", allEntries = true)
  @Transactional
  public Map<String, Object> create(CreateRegistrationRequest request, String idempotencyKey) {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    if (!redisRateLimiter.allow("rate:registration:create:user:" + user.userId(), 3, Duration.ofSeconds(10))) {
      throw new BusinessException(429, "registration requests too frequent");
    }
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      String key = "idem:registration:create:" + user.userId() + ":" + idempotencyKey.trim();
      if (!redisIdempotencyGuard.acquire(key, Duration.ofSeconds(60))) {
        throw new BusinessException(409, "duplicate registration request is processing");
      }
    }
    Doctor doctor = doctorRepository.findById(request.doctorId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    departmentRepository.findById(request.departmentId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (request.slotId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    AppointmentSlot slot = appointmentSlotRepository.findByIdForUpdate(request.slotId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (registrationRepository.existsByPatientIdAndSlotIdAndStatusNot(user.userId(), slot.getId(), "CANCELLED")) {
      throw new BusinessException(ErrorCode.CONFLICT);
    }
    if (!"AVAILABLE".equalsIgnoreCase(slot.getStatus())
        || slot.getRemainingCapacity() == null
        || slot.getRemainingCapacity() <= 0
        || !slot.getDoctorId().equals(request.doctorId())
        || !slot.getDepartmentId().equals(request.departmentId())) {
      throw new BusinessException(ErrorCode.CONFLICT);
    }
    slot.setRemainingCapacity(slot.getRemainingCapacity() - 1);
    if (slot.getRemainingCapacity() <= 0) {
      slot.setStatus("FULL");
    }
    slot.setUpdatedAt(LocalDateTime.now());
    appointmentSlotRepository.save(slot);
    VisitorSnapshot visitor = resolveVisitor(request, user);
    Registration registration = new Registration();
    registration.setPatientId(user.userId());
    registration.setVisitorId(visitor.id());
    registration.setVisitorType(visitor.type());
    registration.setVisitorName(visitor.name());
    registration.setVisitorRelationship(visitor.relationship());
    registration.setVisitorGender(visitor.gender());
    registration.setVisitorAge(visitor.age());
    registration.setDoctorId(doctor.getId());
    registration.setDepartmentId(request.departmentId());
    registration.setTriageRecordId(request.triageRecordId());
    registration.setSlotId(slot.getId());
    registration.setAppointmentTime(slot.getStartTime());
    registration.setStatus("CREATED");
    registration.setUpdatedAt(LocalDateTime.now());
    Registration saved = registrationRepository.save(registration);
    publishRegistrationCreated(saved, user);
    return registrationView(saved);
  }

  public List<Map<String, Object>> list() {
    AuthenticatedUser user = currentUserService.get();
    List<Registration> registrations;
    if (user.role() == RoleType.PATIENT) {
      registrations = registrationRepository.findByPatientId(user.userId());
    } else if (user.role() == RoleType.DOCTOR) {
      registrations = registrationRepository.findByDoctorId(user.userId());
    } else {
      registrations = registrationRepository.findAll();
    }
    return registrations.stream().map(this::registrationView).toList();
  }

  @CacheEvict(cacheNames = "registration:slots", allEntries = true)
  @Transactional
  public Map<String, Object> cancel(Long registrationId) {
    AuthenticatedUser user = currentUserService.get();
    Registration registration = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (user.role() == RoleType.PATIENT && !registration.getPatientId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (user.role() == RoleType.DOCTOR && !registration.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
      return registrationView(registration);
    }
    if ("COMPLETED".equalsIgnoreCase(registration.getStatus())) {
      throw new BusinessException(400, "已完成的挂号不允许取消");
    }
    registration.setStatus("CANCELLED");
    registration.setUpdatedAt(LocalDateTime.now());
    Registration saved = registrationRepository.save(registration);
    restoreSlotCapacity(saved.getSlotId());
    publishRegistrationCancelled(saved, user);
    return registrationView(saved);
  }

  @Transactional
  public Map<String, Object> complete(Long registrationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!registration.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if ("CANCELLED".equalsIgnoreCase(registration.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT);
    }
    registration.setStatus("COMPLETED");
    registration.setUpdatedAt(LocalDateTime.now());
    return registrationView(registrationRepository.save(registration));
  }

  public List<Map<String, Object>> slots() {
    currentUserService.get();
    return registrationSlotQueryService.availableSlots();
  }

  public Map<String, Object> registrationView(Registration registration) {
    Patient patient = patientRepository.findById(registration.getPatientId()).orElse(null);
    Doctor doctor = doctorRepository.findById(registration.getDoctorId()).orElse(null);
    Department department = departmentRepository.findById(registration.getDepartmentId()).orElse(null);
    String patientName = text(registration.getVisitorName(), patient == null ? "" : patient.getName());
    String patientGender = text(registration.getVisitorGender(), patient == null ? "" : patient.getGender());
    Integer patientAge = registration.getVisitorAge() == null && patient != null ? patient.getAge() : registration.getVisitorAge();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("registrationId", registration.getId());
    view.put("patientId", registration.getPatientId());
    view.put("patientName", patientName);
    view.put("patientAge", patientAge);
    view.put("patientGender", patientGender);
    view.put("visitorId", registration.getVisitorId() == null ? registration.getPatientId() : registration.getVisitorId());
    view.put("visitorType", text(registration.getVisitorType(), "ACCOUNT"));
    view.put("visitorName", patientName);
    view.put("visitorRelationship", text(registration.getVisitorRelationship(), "本人"));
    view.put("doctorId", registration.getDoctorId());
    view.put("doctorName", doctor == null ? "" : doctor.getName());
    view.put("departmentId", registration.getDepartmentId());
    view.put("departmentName", department == null ? "" : department.getName());
    view.put("createdAt", registration.getCreatedAt() == null ? "" : registration.getCreatedAt().toString());
    view.put("appointmentTime", registration.getAppointmentTime() == null ? "" : registration.getAppointmentTime().toString());
    view.put("slotId", registration.getSlotId() == null ? 0L : registration.getSlotId());
    view.put("status", registration.getStatus());
    view.put("triageRecordId", registration.getTriageRecordId() == null ? 0L : registration.getTriageRecordId());
    return view;
  }

  private VisitorSnapshot resolveVisitor(CreateRegistrationRequest request, AuthenticatedUser user) {
    Patient patient = patientRepository.findById(user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"VISITOR".equalsIgnoreCase(text(request.visitorType(), "")) || request.visitorId() == null) {
      return new VisitorSnapshot(patient.getId(), "ACCOUNT", patient.getName(), "本人", patient.getGender(), patient.getAge());
    }
    PatientVisitor visitor = patientVisitorRepository.findByIdAndOwnerPatientId(request.visitorId(), user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return new VisitorSnapshot(visitor.getId(), "VISITOR", visitor.getName(), visitor.getRelationship(), visitor.getGender(), visitor.getAge());
  }

  private String text(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private record VisitorSnapshot(Long id, String type, String name, String relationship, String gender, Integer age) {
  }

  private void restoreSlotCapacity(Long slotId) {
    if (slotId == null) {
      return;
    }
    AppointmentSlot slot = appointmentSlotRepository.findByIdForUpdate(slotId).orElse(null);
    if (slot == null || slot.getCapacity() == null || slot.getRemainingCapacity() == null) {
      return;
    }
    if (slot.getRemainingCapacity() < slot.getCapacity()) {
      slot.setRemainingCapacity(slot.getRemainingCapacity() + 1);
      if (slot.getRemainingCapacity() > 0 && !"DISABLED".equalsIgnoreCase(slot.getStatus())) {
        slot.setStatus("AVAILABLE");
      }
      slot.setUpdatedAt(LocalDateTime.now());
      appointmentSlotRepository.save(slot);
    }
  }

  private void publishRegistrationCreated(Registration registration, AuthenticatedUser user) {
    Map<String, Object> payload = registrationPayload(registration, "REGISTRATION_CREATED", "挂号成功提醒", "患者已完成挂号，请关注候诊队列。");
    domainEventPublisher.publishNotification(DomainEventNames.REGISTRATION_CREATED, payload);
    domainEventPublisher.publishAudit(DomainEventNames.REGISTRATION_CREATED, auditPayload(
        user, "REGISTRATION_CREATED", "REGISTRATION", registration.getId(), payload));
  }

  private void publishRegistrationCancelled(Registration registration, AuthenticatedUser user) {
    Map<String, Object> payload = registrationPayload(registration, "REGISTRATION_CANCELLED", "挂号取消提醒", "患者已取消挂号，请留意号源变化。");
    domainEventPublisher.publishNotification(DomainEventNames.REGISTRATION_CANCELLED, payload);
    domainEventPublisher.publishAudit(DomainEventNames.REGISTRATION_CANCELLED, auditPayload(
        user, "REGISTRATION_CANCELLED", "REGISTRATION", registration.getId(), payload));
  }

  private Map<String, Object> registrationPayload(Registration registration, String type, String title, String content) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", registration.getDoctorId());
    payload.put("patientId", registration.getPatientId());
    payload.put("registrationId", registration.getId());
    payload.put("triageRecordId", registration.getTriageRecordId() == null ? 0L : registration.getTriageRecordId());
    payload.put("type", type);
    payload.put("title", title);
    payload.put("content", content);
    return payload;
  }

  private Map<String, Object> auditPayload(
      AuthenticatedUser user,
      String action,
      String resourceType,
      Long resourceId,
      Map<String, Object> detail
  ) {
    Map<String, Object> payload = new LinkedHashMap<>(detail);
    payload.put("actorType", user.role().name());
    payload.put("actorId", user.userId());
    payload.put("action", action);
    payload.put("resourceType", resourceType);
    payload.put("resourceId", String.valueOf(resourceId));
    payload.put("outcome", "SUCCESS");
    return payload;
  }

}


