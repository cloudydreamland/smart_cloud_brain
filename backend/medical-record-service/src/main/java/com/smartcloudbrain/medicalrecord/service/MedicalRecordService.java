package com.smartcloudbrain.medicalrecord.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
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
    AuthenticatedUser user = currentUserService.get();
    Registration registration = registrationRepository.findById(request.registrationId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    MedicalRecord record = medicalRecordRepository.findByRegistrationId(request.registrationId()).orElseGet(MedicalRecord::new);
    record.setRegistrationId(registration.getId());
    record.setPatientId(registration.getPatientId());
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
    List<MedicalRecord> records;
    if (user.role() == RoleType.PATIENT) {
      records = medicalRecordRepository.findByPatientId(user.userId());
    } else if (user.role() == RoleType.DOCTOR) {
      records = medicalRecordRepository.findByDoctorId(user.userId());
    } else {
      records = medicalRecordRepository.findAll();
    }
    return records.stream().map(this::recordView).toList();
  }

  public Map<String, Object> detail(Long id) {
    return recordView(medicalRecordRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  public Map<String, Object> recordView(MedicalRecord record) {
    Patient patient = patientRepository.findById(record.getPatientId()).orElse(null);
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("medicalRecordId", record.getId());
    view.put("registrationId", record.getRegistrationId());
    view.put("patientId", record.getPatientId());
    view.put("patientName", patient == null ? "" : patient.getName());
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
}


