package com.smartcloudbrain.patient.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.patient.entity.Patient;
import com.smartcloudbrain.patient.dto.PatientProfileSaveRequest;
import com.smartcloudbrain.patient.repository.PatientRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

  private final PatientRepository patientRepository;
  private final CurrentUserService currentUserService;

  public PatientService(PatientRepository patientRepository, CurrentUserService currentUserService) {
    this.patientRepository = patientRepository;
    this.currentUserService = currentUserService;
  }

  public Map<String, Object> currentPatientInfo() {
    AuthenticatedUser user = currentUserService.get();
    Patient patient = patientRepository.findById(user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return patientView(patient);
  }

  public Map<String, Object> patientSummary(Long patientId) {
    Patient patient = patientRepository.findById(patientId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return patientView(patient);
  }

  @Transactional
  public Map<String, Object> saveProfile(PatientProfileSaveRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    Patient patient = patientRepository.findById(user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    patient.setName(request.name());
    patient.setGender(request.gender());
    patient.setAge(request.age());
    patient.setAllergyHistory(request.allergyHistory());
    patient.setPastHistory(request.pastHistory());
    patient.setUpdatedAt(LocalDateTime.now());
    return patientView(patientRepository.save(patient));
  }

  public Map<String, Object> patientView(Patient patient) {
    return Map.of(
        "id", patient.getId(),
        "name", patient.getName(),
        "phone", patient.getPhone(),
        "gender", patient.getGender() == null ? "" : patient.getGender(),
        "age", patient.getAge() == null ? 0 : patient.getAge(),
        "allergyHistory", patient.getAllergyHistory() == null ? "" : patient.getAllergyHistory(),
        "pastHistory", patient.getPastHistory() == null ? "" : patient.getPastHistory(),
        "updatedAt", patient.getUpdatedAt() == null ? "" : patient.getUpdatedAt().toString()
    );
  }
}

