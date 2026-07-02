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
import com.smartcloudbrain.registration.entity.RegistrationOrder;
import com.smartcloudbrain.registration.repository.AppointmentSlotRepository;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import com.smartcloudbrain.registration.repository.PatientRepository;
import com.smartcloudbrain.registration.repository.PatientVisitorRepository;
import com.smartcloudbrain.registration.repository.RegistrationOrderRepository;
import com.smartcloudbrain.registration.repository.RegistrationRepository;
import com.smartcloudbrain.registration.event.DomainEventPublisher;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.math.BigDecimal;
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
  private final RegistrationOrderRepository registrationOrderRepository;
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
      RegistrationOrderRepository registrationOrderRepository,
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
    this.registrationOrderRepository = registrationOrderRepository;
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
    SubjectSnapshot subject = resolveSubject(request, user);
    if (registrationRepository.existsByOwnerPatientIdAndSubjectTypeAndSubjectIdAndSlotIdAndStatusNotIn(
        user.userId(), subject.type(), subject.id(), slot.getId(), List.of("CANCELLED", "REFUNDED"))) {
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
    Registration registration = new Registration();
    registration.setPatientId(user.userId());
    registration.setOwnerPatientId(user.userId());
    registration.setSubjectType(subject.type());
    registration.setSubjectId(subject.id());
    registration.setSubjectName(subject.name());
    registration.setSubjectRelationship(subject.relationship());
    registration.setSubjectGender(subject.gender());
    registration.setSubjectAge(subject.age());
    registration.setVisitorId(subject.id());
    registration.setVisitorType(subject.type());
    registration.setVisitorName(subject.name());
    registration.setVisitorRelationship(subject.relationship());
    registration.setVisitorGender(subject.gender());
    registration.setVisitorAge(subject.age());
    registration.setDoctorId(doctor.getId());
    registration.setDepartmentId(request.departmentId());
    registration.setTriageRecordId(request.triageRecordId());
    registration.setSlotId(slot.getId());
    registration.setAppointmentTime(slot.getStartTime());
    registration.setStatus(RegistrationStatus.PENDING_PAYMENT.name());
    registration.setUpdatedAt(LocalDateTime.now());
    Registration saved = registrationRepository.save(registration);
    RegistrationOrder order = createOrder(saved);
    publishRegistrationCreated(saved, user);
    return registrationView(saved, order, user);
  }

  public List<Map<String, Object>> list() {
    AuthenticatedUser user = currentUserService.get();
    List<Registration> registrations;
    if (user.role() == RoleType.PATIENT) {
      registrations = registrationRepository.findByOwnerPatientId(user.userId());
    } else if (user.role() == RoleType.DOCTOR) {
      registrations = registrationRepository.findByDoctorId(user.userId());
    } else {
      registrations = registrationRepository.findAll();
    }
    return registrations.stream().map(registration -> registrationView(registration, user)).toList();
  }

  @CacheEvict(cacheNames = "registration:slots", allEntries = true)
  @Transactional
  public Map<String, Object> cancel(Long registrationId) {
    AuthenticatedUser user = currentUserService.get();
    Registration registration = requireOperableRegistration(registrationId, user);
    RegistrationStatus status = RegistrationStatus.from(registration.getStatus());
    if (!status.canCancel()) {
      throw new BusinessException(400, "registration cannot be cancelled in current status");
    }
    RegistrationOrder order = orderFor(registration).orElse(null);
    LocalDateTime now = LocalDateTime.now();
    if (status == RegistrationStatus.PAID) {
      registration.setStatus(RegistrationStatus.REFUNDING.name());
      updateOrder(order, PaymentStatus.REFUNDING, null, null);
    } else {
      registration.setStatus(RegistrationStatus.CANCELLED.name());
      updateOrder(order, PaymentStatus.CLOSED, null, now);
    }
    registration.setUpdatedAt(now);
    Registration saved = registrationRepository.save(registration);
    restoreSlotCapacity(saved.getSlotId());
    publishRegistrationCancelled(saved, user);
    return registrationView(saved, order, user);
  }

  @Transactional
  public Map<String, Object> pay(Long registrationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    Registration registration = requireOperableRegistration(registrationId, user);
    RegistrationStatus status = RegistrationStatus.from(registration.getStatus());
    if (!status.canPay()) {
      throw new BusinessException(400, "registration cannot be paid in current status");
    }
    RegistrationOrder order = requireOrder(registration);
    if (PaymentStatus.from(order.getPaymentStatus()) != PaymentStatus.UNPAID) {
      throw new BusinessException(400, "order cannot be paid in current status");
    }
    LocalDateTime now = LocalDateTime.now();
    registration.setStatus(RegistrationStatus.PAID.name());
    registration.setUpdatedAt(now);
    updateOrder(order, PaymentStatus.PAID, now, null);
    Registration saved = registrationRepository.save(registration);
    publishRegistrationStatusChanged(saved, user, DomainEventNames.REGISTRATION_PAID, "REGISTRATION_PAID");
    return registrationView(saved, order, user);
  }

  @Transactional
  public Map<String, Object> checkIn(Long registrationId) {
    AuthenticatedUser user = currentUserService.get();
    Registration registration = requireOperableRegistration(registrationId, user);
    if (!RegistrationStatus.from(registration.getStatus()).canCheckIn()) {
      throw new BusinessException(400, "registration cannot check in in current status");
    }
    return transition(registration, user, RegistrationStatus.CHECKED_IN, DomainEventNames.REGISTRATION_CHECKED_IN, "REGISTRATION_CHECKED_IN");
  }

  @Transactional
  public Map<String, Object> joinQueue(Long registrationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = requireOperableRegistration(registrationId, user);
    if (!RegistrationStatus.from(registration.getStatus()).canJoinQueue()) {
      throw new BusinessException(400, "registration cannot join queue in current status");
    }
    return transition(registration, user, RegistrationStatus.WAITING, DomainEventNames.REGISTRATION_WAITING, "REGISTRATION_WAITING");
  }

  @Transactional
  public Map<String, Object> call(Long registrationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = requireOperableRegistration(registrationId, user);
    if (!RegistrationStatus.from(registration.getStatus()).canCall()) {
      throw new BusinessException(400, "registration cannot be called in current status");
    }
    return transition(registration, user, RegistrationStatus.CALLED, DomainEventNames.REGISTRATION_CALLED, "REGISTRATION_CALLED");
  }

  @Transactional
  public Map<String, Object> startConsultation(Long registrationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = requireOperableRegistration(registrationId, user);
    if (!RegistrationStatus.from(registration.getStatus()).canStartConsultation()) {
      throw new BusinessException(400, "registration cannot start consultation in current status");
    }
    return transition(registration, user, RegistrationStatus.IN_CONSULTATION, DomainEventNames.REGISTRATION_STARTED, "REGISTRATION_STARTED");
  }

  @Transactional
  public Map<String, Object> refund(Long registrationId) {
    AuthenticatedUser user = currentUserService.get();
    Registration registration = requireOperableRegistration(registrationId, user);
    if (!RegistrationStatus.from(registration.getStatus()).canRefund()) {
      throw new BusinessException(400, "registration cannot be refunded in current status");
    }
    RegistrationOrder order = requireOrder(registration);
    LocalDateTime now = LocalDateTime.now();
    registration.setStatus(RegistrationStatus.REFUNDED.name());
    registration.setUpdatedAt(now);
    updateOrder(order, PaymentStatus.REFUNDED, null, now);
    Registration saved = registrationRepository.save(registration);
    publishRegistrationStatusChanged(saved, user, DomainEventNames.REGISTRATION_REFUNDED, "REGISTRATION_REFUNDED");
    return registrationView(saved, order, user);
  }

  @CacheEvict(cacheNames = "registration:slots", allEntries = true)
  @Transactional
  private Map<String, Object> cancelLegacy(Long registrationId) {
    AuthenticatedUser user = currentUserService.get();
    Registration registration = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (user.role() == RoleType.PATIENT && !ownerPatientId(registration).equals(user.userId())) {
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
    Registration registration = requireOperableRegistration(registrationId, user);
    if (!RegistrationStatus.from(registration.getStatus()).canComplete()) {
      throw new BusinessException(400, "registration cannot be completed in current status");
    }
    return transition(registration, user, RegistrationStatus.COMPLETED, DomainEventNames.REGISTRATION_COMPLETED, "REGISTRATION_COMPLETED");
  }

  @Transactional
  private Map<String, Object> completeLegacy(Long registrationId) {
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
    return registrationView(registration, orderFor(registration).orElse(null), currentUserService.get());
  }

  private Map<String, Object> registrationView(Registration registration, AuthenticatedUser user) {
    return registrationView(registration, orderFor(registration).orElse(null), user);
  }

  private Map<String, Object> registrationView(Registration registration, RegistrationOrder order, AuthenticatedUser user) {
    Patient patient = patientRepository.findById(registration.getPatientId()).orElse(null);
    Doctor doctor = doctorRepository.findById(registration.getDoctorId()).orElse(null);
    Department department = departmentRepository.findById(registration.getDepartmentId()).orElse(null);
    String patientName = text(registration.getSubjectName(), text(registration.getVisitorName(), patient == null ? "" : patient.getName()));
    String patientGender = text(registration.getSubjectGender(), text(registration.getVisitorGender(), patient == null ? "" : patient.getGender()));
    Integer patientAge = registration.getSubjectAge() == null
        ? (registration.getVisitorAge() == null && patient != null ? patient.getAge() : registration.getVisitorAge())
        : registration.getSubjectAge();
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("registrationId", registration.getId());
    view.put("patientId", registration.getPatientId());
    view.put("ownerPatientId", ownerPatientId(registration));
    view.put("subjectType", subjectType(registration));
    view.put("subjectId", subjectId(registration));
    view.put("subjectName", patientName);
    view.put("subjectRelationship", text(registration.getSubjectRelationship(), text(registration.getVisitorRelationship(), "本人")));
    view.put("patientName", patientName);
    view.put("patientAge", patientAge);
    view.put("patientGender", patientGender);
    view.put("visitorId", subjectId(registration));
    view.put("visitorType", subjectType(registration));
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
    view.put("orderId", order == null ? 0L : order.getId());
    view.put("orderNo", order == null ? "" : text(order.getOrderNo(), ""));
    view.put("amount", order == null || order.getAmount() == null ? BigDecimal.ZERO : order.getAmount());
    view.put("paymentStatus", order == null ? fallbackPaymentStatus(registration).name() : PaymentStatus.from(order.getPaymentStatus()).name());
    view.put("paymentMethod", order == null ? "" : text(order.getPaymentMethod(), ""));
    view.put("paidAt", order == null || order.getPaidAt() == null ? "" : order.getPaidAt().toString());
    view.put("closedAt", order == null || order.getClosedAt() == null ? "" : order.getClosedAt().toString());
    view.putAll(actionFlags(registration, user));
    return view;
  }

  private SubjectSnapshot resolveSubject(CreateRegistrationRequest request, AuthenticatedUser user) {
    Patient patient = patientRepository.findById(user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    String requestedType = text(request.subjectType(), "");
    Long requestedId = request.subjectId();
    if (requestedType.isBlank()) {
      requestedType = text(request.visitorType(), "");
      requestedId = request.visitorId();
    }
    requestedType = requestedType.isBlank() ? "ACCOUNT" : requestedType.trim().toUpperCase();
    if ("ACCOUNT".equals(requestedType)) {
      Long subjectId = requestedId == null ? user.userId() : requestedId;
      if (!subjectId.equals(user.userId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
      return new SubjectSnapshot(patient.getId(), "ACCOUNT", patient.getName(), "本人", patient.getGender(), patient.getAge());
    }
    if (!"VISITOR".equals(requestedType) || requestedId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    PatientVisitor visitor = patientVisitorRepository.findByIdAndOwnerPatientId(requestedId, user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    return new SubjectSnapshot(visitor.getId(), "VISITOR", visitor.getName(), visitor.getRelationship(), visitor.getGender(), visitor.getAge());
  }

  private String text(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private Long ownerPatientId(Registration registration) {
    return registration.getOwnerPatientId() == null ? registration.getPatientId() : registration.getOwnerPatientId();
  }

  private String subjectType(Registration registration) {
    return text(registration.getSubjectType(), text(registration.getVisitorType(), "ACCOUNT"));
  }

  private Long subjectId(Registration registration) {
    if (registration.getSubjectId() != null) {
      return registration.getSubjectId();
    }
    if (registration.getVisitorId() != null) {
      return registration.getVisitorId();
    }
    return ownerPatientId(registration);
  }

  private record SubjectSnapshot(Long id, String type, String name, String relationship, String gender, Integer age) {
  }

  private RegistrationOrder createOrder(Registration registration) {
    RegistrationOrder order = new RegistrationOrder();
    order.setOrderNo("REG-" + registration.getId() + "-" + System.currentTimeMillis());
    order.setRegistrationId(registration.getId());
    order.setOwnerPatientId(ownerPatientId(registration));
    order.setSubjectType(subjectType(registration));
    order.setSubjectId(subjectId(registration));
    order.setAmount(BigDecimal.ZERO);
    order.setPaymentStatus(PaymentStatus.UNPAID.name());
    order.setPaymentMethod("MOCK");
    order.setUpdatedAt(LocalDateTime.now());
    return registrationOrderRepository.save(order);
  }

  private java.util.Optional<RegistrationOrder> orderFor(Registration registration) {
    if (registration == null || registration.getId() == null) {
      return java.util.Optional.empty();
    }
    return registrationOrderRepository.findFirstByRegistrationIdOrderByIdDesc(registration.getId());
  }

  private RegistrationOrder requireOrder(Registration registration) {
    return orderFor(registration).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private void updateOrder(RegistrationOrder order, PaymentStatus status, LocalDateTime paidAt, LocalDateTime closedAt) {
    if (order == null) {
      return;
    }
    order.setPaymentStatus(status.name());
    if (paidAt != null) {
      order.setPaidAt(paidAt);
    }
    if (closedAt != null) {
      order.setClosedAt(closedAt);
    }
    order.setUpdatedAt(LocalDateTime.now());
    registrationOrderRepository.save(order);
  }

  private Registration requireOperableRegistration(Long registrationId, AuthenticatedUser user) {
    Registration registration = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (user.role() == RoleType.PATIENT && !ownerPatientId(registration).equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (user.role() == RoleType.DOCTOR && !registration.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return registration;
  }

  private Map<String, Object> transition(
      Registration registration,
      AuthenticatedUser user,
      RegistrationStatus nextStatus,
      String eventType,
      String action
  ) {
    registration.setStatus(nextStatus.name());
    registration.setUpdatedAt(LocalDateTime.now());
    Registration saved = registrationRepository.save(registration);
    publishRegistrationStatusChanged(saved, user, eventType, action);
    return registrationView(saved, user);
  }

  private Map<String, Object> actionFlags(Registration registration, AuthenticatedUser user) {
    RegistrationStatus status = RegistrationStatus.from(registration.getStatus());
    boolean owner = user.role() == RoleType.PATIENT && ownerPatientId(registration).equals(user.userId());
    boolean doctor = user.role() == RoleType.DOCTOR && registration.getDoctorId().equals(user.userId());
    boolean admin = user.role() == RoleType.ADMIN;
    Map<String, Object> flags = new LinkedHashMap<>();
    flags.put("canPay", owner && status.canPay());
    flags.put("canCancel", (owner || admin) && status.canCancel());
    flags.put("canCheckIn", (owner || doctor || admin) && status.canCheckIn());
    flags.put("canJoinQueue", (doctor || admin) && status.canJoinQueue());
    flags.put("canCall", (doctor || admin) && status.canCall());
    flags.put("canStartConsultation", (doctor || admin) && status.canStartConsultation());
    flags.put("canComplete", (doctor || admin) && status.canComplete());
    flags.put("canRefund", (owner || admin) && status.canRefund());
    return flags;
  }

  private PaymentStatus fallbackPaymentStatus(Registration registration) {
    return switch (RegistrationStatus.from(registration.getStatus())) {
      case PENDING_PAYMENT -> PaymentStatus.UNPAID;
      case CANCELLED -> PaymentStatus.CLOSED;
      case REFUNDING -> PaymentStatus.REFUNDING;
      case REFUNDED -> PaymentStatus.REFUNDED;
      default -> PaymentStatus.PAID;
    };
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

  private void publishRegistrationStatusChanged(
      Registration registration,
      AuthenticatedUser user,
      String eventType,
      String action
  ) {
    Map<String, Object> payload = registrationPayload(registration, action, "", "");
    payload.put("status", registration.getStatus());
    domainEventPublisher.publishAudit(eventType, auditPayload(
        user, action, "REGISTRATION", registration.getId(), payload));
  }

  private Map<String, Object> registrationPayload(Registration registration, String type, String title, String content) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", registration.getDoctorId());
    payload.put("patientId", registration.getPatientId());
    payload.put("ownerPatientId", ownerPatientId(registration));
    payload.put("subjectType", subjectType(registration));
    payload.put("subjectId", subjectId(registration));
    payload.put("subjectName", text(registration.getSubjectName(), text(registration.getVisitorName(), "")));
    payload.put("subjectRelationship", text(registration.getSubjectRelationship(), text(registration.getVisitorRelationship(), "")));
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


