package com.smartcloudbrain.registration.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.registration.dto.registration.CreateRegistrationRequest;
import com.smartcloudbrain.registration.entity.Department;
import com.smartcloudbrain.registration.entity.Doctor;
import com.smartcloudbrain.registration.entity.Patient;
import com.smartcloudbrain.registration.entity.Registration;
import com.smartcloudbrain.registration.repository.DepartmentRepository;
import com.smartcloudbrain.registration.repository.DoctorRepository;
import com.smartcloudbrain.registration.repository.PatientRepository;
import com.smartcloudbrain.registration.repository.RegistrationRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

  private final RegistrationRepository registrationRepository;
  private final DoctorRepository doctorRepository;
  private final PatientRepository patientRepository;
  private final DepartmentRepository departmentRepository;
  private final CurrentUserService currentUserService;

  public RegistrationService(
      RegistrationRepository registrationRepository,
      DoctorRepository doctorRepository,
      PatientRepository patientRepository,
      DepartmentRepository departmentRepository,
      CurrentUserService currentUserService
  ) {
    this.registrationRepository = registrationRepository;
    this.doctorRepository = doctorRepository;
    this.patientRepository = patientRepository;
    this.departmentRepository = departmentRepository;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public Map<String, Object> create(CreateRegistrationRequest request) {
    AuthenticatedUser user = currentUserService.get();
    Doctor doctor = doctorRepository.findById(request.doctorId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    departmentRepository.findById(request.departmentId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    Registration registration = new Registration();
    registration.setPatientId(user.userId());
    registration.setDoctorId(doctor.getId());
    registration.setDepartmentId(request.departmentId());
    registration.setTriageRecordId(request.triageRecordId());
    registration.setAppointmentTime(request.appointmentTime());
    registration.setStatus("CREATED");
    registration.setUpdatedAt(LocalDateTime.now());
    return registrationView(registrationRepository.save(registration));
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

  @Transactional
  public Map<String, Object> cancel(Long registrationId) {
    Registration registration = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    registration.setStatus("CANCELLED");
    registration.setUpdatedAt(LocalDateTime.now());
    return registrationView(registrationRepository.save(registration));
  }

  public Map<String, Object> registrationView(Registration registration) {
    Patient patient = patientRepository.findById(registration.getPatientId()).orElse(null);
    Doctor doctor = doctorRepository.findById(registration.getDoctorId()).orElse(null);
    Department department = departmentRepository.findById(registration.getDepartmentId()).orElse(null);
    return Map.of(
        "registrationId", registration.getId(),
        "patientId", registration.getPatientId(),
        "patientName", patient == null ? "" : patient.getName(),
        "doctorId", registration.getDoctorId(),
        "doctorName", doctor == null ? "" : doctor.getName(),
        "departmentId", registration.getDepartmentId(),
        "departmentName", department == null ? "" : department.getName(),
        "appointmentTime", registration.getAppointmentTime() == null ? "" : registration.getAppointmentTime().toString(),
        "status", registration.getStatus(),
        "triageRecordId", registration.getTriageRecordId() == null ? 0L : registration.getTriageRecordId()
    );
  }
}


