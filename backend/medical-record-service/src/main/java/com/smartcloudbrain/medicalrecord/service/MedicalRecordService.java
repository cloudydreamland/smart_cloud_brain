package com.smartcloudbrain.medicalrecord.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.aiapi.dto.MedicalRecordGenerateRequest;
import com.smartcloudbrain.medicalrecord.dto.medical.MedicalRecordSaveRequest;
import com.smartcloudbrain.medicalrecord.entity.MedicalRecord;
import com.smartcloudbrain.medicalrecord.entity.Patient;
import com.smartcloudbrain.medicalrecord.entity.Registration;
import com.smartcloudbrain.medicalrecord.repository.MedicalRecordRepository;
import com.smartcloudbrain.medicalrecord.repository.PatientRepository;
import com.smartcloudbrain.medicalrecord.repository.RegistrationRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {

  private final MedicalRecordRepository medicalRecordRepository;
  private final RegistrationRepository registrationRepository;
  private final PatientRepository patientRepository;
  private final CurrentUserService currentUserService;

  public MedicalRecordService(
      MedicalRecordRepository medicalRecordRepository,
      RegistrationRepository registrationRepository,
      PatientRepository patientRepository,
      CurrentUserService currentUserService
  ) {
    this.medicalRecordRepository = medicalRecordRepository;
    this.registrationRepository = registrationRepository;
    this.patientRepository = patientRepository;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public Map<String, Object> save(MedicalRecordSaveRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = registrationRepository.findById(request.registrationId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!registration.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    MedicalRecord record = medicalRecordRepository.findByRegistrationId(request.registrationId()).orElseGet(MedicalRecord::new);
    record.setRegistrationId(registration.getId());
    record.setPatientId(registration.getPatientId());
    record.setOwnerPatientId(ownerPatientId(registration));
    record.setSubjectType(subjectType(registration));
    record.setSubjectId(subjectId(registration));
    record.setSubjectName(subjectName(registration));
    record.setSubjectRelationship(text(registration.getSubjectRelationship(), "本人"));
    record.setSubjectGender(registration.getSubjectGender());
    record.setSubjectAge(registration.getSubjectAge());
    record.setDoctorId(user.role() == RoleType.DOCTOR ? user.userId() : registration.getDoctorId());
    record.setChiefComplaint(request.chiefComplaint());
    record.setPresentIllness(request.presentIllness());
    record.setPastHistory(request.pastHistory());
    record.setPhysicalExam(request.physicalExam());
    record.setDiagnosis(request.diagnosis());
    record.setTreatmentAdvice(request.treatmentAdvice());
    record.setAiGenerated(request.aiGenerated() == null || request.aiGenerated());
    return recordView(medicalRecordRepository.save(record));
  }

  public List<Map<String, Object>> list() {
    AuthenticatedUser user = currentUserService.get();
    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    List<MedicalRecord> records;
    if (user.role() == RoleType.PATIENT) {
      records = medicalRecordRepository.findByOwnerPatientId(user.userId(), sort);
    } else if (user.role() == RoleType.DOCTOR) {
      records = medicalRecordRepository.findByDoctorId(user.userId(), sort);
    } else {
      records = medicalRecordRepository.findAll(sort);
    }
    return records.stream().map(this::recordView).toList();
  }

  public List<Map<String, Object>> recordsByPatient(Long patientId) {
    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    return medicalRecordRepository.findByOwnerPatientId(patientId, sort).stream()
        .map(this::recordView)
        .toList();
  }

  public Map<String, Object> detail(Long id) {
    MedicalRecord record = medicalRecordRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    AuthenticatedUser user = currentUserService.get();
    if (user.role() == RoleType.PATIENT && !ownerPatientId(record).equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (user.role() == RoleType.DOCTOR && !record.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return recordView(record);
  }

  public void requireDoctorRegistration(Long registrationId) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!registration.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
  }

  public MedicalRecordGenerateRequest buildGenerateRequest(MedicalRecordGenerateRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Registration registration = registrationRepository.findById(request.registrationId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!registration.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Patient patient = patientRepository.findById(ownerPatientId(registration)).orElse(null);
    String subjectName = subjectName(registration);
    Integer subjectAge = registration.getSubjectAge() == null && patient != null ? patient.getAge() : registration.getSubjectAge();
    String subjectGender = text(registration.getSubjectGender(), patient == null ? "" : patient.getGender());
    return new MedicalRecordGenerateRequest(
        registration.getId(),
        request.departmentCode(),
        request.dialogueText(),
        ownerPatientId(registration),
        subjectName,
        subjectAge,
        subjectGender,
        "ACCOUNT".equals(subjectType(registration)) && patient != null ? patient.getAllergyHistory() : "",
        "ACCOUNT".equals(subjectType(registration)) && patient != null ? patient.getPastHistory() : "",
        registration.getDoctorId(),
        "",
        registration.getAppointmentTime() == null ? "" : registration.getAppointmentTime().toString()
    );
  }

  public Map<String, Object> recordView(MedicalRecord record) {
    Patient patient = patientRepository.findById(ownerPatientId(record)).orElse(null);
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("medicalRecordId", record.getId());
    view.put("registrationId", record.getRegistrationId());
    view.put("patientId", record.getPatientId());
    view.put("ownerPatientId", ownerPatientId(record));
    view.put("subjectType", subjectType(record));
    view.put("subjectId", subjectId(record));
    view.put("subjectName", text(record.getSubjectName(), patient == null ? "" : patient.getName()));
    view.put("subjectRelationship", text(record.getSubjectRelationship(), "本人"));
    view.put("patientName", text(record.getSubjectName(), patient == null ? "" : patient.getName()));
    view.put("doctorId", record.getDoctorId());
    view.put("chiefComplaint", record.getChiefComplaint());
    view.put("presentIllness", record.getPresentIllness() == null ? "" : record.getPresentIllness());
    view.put("pastHistory", record.getPastHistory() == null ? "" : record.getPastHistory());
    view.put("physicalExam", record.getPhysicalExam() == null ? "" : record.getPhysicalExam());
    view.put("diagnosis", record.getDiagnosis());
    view.put("treatmentAdvice", record.getTreatmentAdvice() == null ? "" : record.getTreatmentAdvice());
    view.put("aiGenerated", Boolean.TRUE.equals(record.getAiGenerated()));
    view.put("createdAt", record.getCreatedAt() == null ? "" : record.getCreatedAt().toString());
    return view;
  }

  private Long ownerPatientId(Registration registration) {
    return registration.getOwnerPatientId() == null ? registration.getPatientId() : registration.getOwnerPatientId();
  }

  private Long ownerPatientId(MedicalRecord record) {
    return record.getOwnerPatientId() == null ? record.getPatientId() : record.getOwnerPatientId();
  }

  private String subjectType(Registration registration) {
    return text(registration.getSubjectType(), "ACCOUNT");
  }

  private String subjectType(MedicalRecord record) {
    return text(record.getSubjectType(), "ACCOUNT");
  }

  private Long subjectId(Registration registration) {
    return registration.getSubjectId() == null ? ownerPatientId(registration) : registration.getSubjectId();
  }

  private Long subjectId(MedicalRecord record) {
    return record.getSubjectId() == null ? ownerPatientId(record) : record.getSubjectId();
  }

  private String subjectName(Registration registration) {
    return text(registration.getSubjectName(), "");
  }

  private String text(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}

