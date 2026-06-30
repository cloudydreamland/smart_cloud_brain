package com.smartcloudbrain.patient.service;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.patient.dto.PatientProfileSaveRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorDeleteRequest;
import com.smartcloudbrain.patient.dto.PatientVisitorSaveRequest;
import com.smartcloudbrain.patient.entity.Patient;
import com.smartcloudbrain.patient.entity.PatientVisitor;
import com.smartcloudbrain.patient.repository.PatientRepository;
import com.smartcloudbrain.patient.repository.PatientVisitorRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

  private final PatientRepository patientRepository;
  private final PatientVisitorRepository patientVisitorRepository;
  private final CurrentUserService currentUserService;

  public PatientService(
      PatientRepository patientRepository,
      PatientVisitorRepository patientVisitorRepository,
      CurrentUserService currentUserService
  ) {
    this.patientRepository = patientRepository;
    this.patientVisitorRepository = patientVisitorRepository;
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
    patient.setAddress(request.address());
    patient.setEmergencyContact(request.emergencyContact());
    patient.setEmergencyPhone(request.emergencyPhone());
    patient.setBloodType(request.bloodType());
    patient.setHeightCm(request.heightCm());
    patient.setWeightKg(request.weightKg());
    patient.setUpdatedAt(LocalDateTime.now());
    return patientView(patientRepository.save(patient));
  }

  public List<Map<String, Object>> visitors() {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    Patient patient = patientRepository.findById(user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    List<Map<String, Object>> rows = new ArrayList<>();
    Map<String, Object> account = patientView(patient);
    account.put("visitorType", "ACCOUNT");
    account.put("relationship", "本人");
    account.put("editable", false);
    rows.add(account);
    patientVisitorRepository.findByOwnerPatientIdOrderByIdAsc(user.userId()).stream()
        .map(this::visitorView)
        .forEach(rows::add);
    return rows;
  }

  @Transactional
  public Map<String, Object> saveVisitor(PatientVisitorSaveRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    PatientVisitor visitor = request.id() == null
        ? new PatientVisitor()
        : patientVisitorRepository.findByIdAndOwnerPatientId(request.id(), user.userId())
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    visitor.setOwnerPatientId(user.userId());
    visitor.setName(request.name());
    visitor.setRelationship(request.relationship());
    visitor.setPhone(request.phone());
    visitor.setGender(request.gender());
    visitor.setAge(request.age());
    visitor.setAddress(request.address());
    visitor.setEmergencyContact(request.emergencyContact());
    visitor.setEmergencyPhone(request.emergencyPhone());
    visitor.setBloodType(request.bloodType());
    visitor.setHeightCm(request.heightCm());
    visitor.setWeightKg(request.weightKg());
    visitor.setAllergyHistory(request.allergyHistory());
    visitor.setPastHistory(request.pastHistory());
    visitor.setUpdatedAt(LocalDateTime.now());
    return visitorView(patientVisitorRepository.save(visitor));
  }

  @Transactional
  public Map<String, Object> deleteVisitor(PatientVisitorDeleteRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.PATIENT);
    PatientVisitor visitor = patientVisitorRepository.findByIdAndOwnerPatientId(request.id(), user.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    patientVisitorRepository.delete(visitor);
    return Map.of("deleted", true, "id", request.id());
  }

  public Map<String, Object> patientView(Patient patient) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", patient.getId());
    view.put("name", patient.getName());
    view.put("phone", value(patient.getPhone()));
    view.put("email", value(patient.getEmail()));
    view.put("gender", value(patient.getGender()));
    view.put("age", patient.getAge() == null ? 0 : patient.getAge());
    view.put("allergyHistory", value(patient.getAllergyHistory()));
    view.put("pastHistory", value(patient.getPastHistory()));
    view.put("address", value(patient.getAddress()));
    view.put("emergencyContact", value(patient.getEmergencyContact()));
    view.put("emergencyPhone", value(patient.getEmergencyPhone()));
    view.put("bloodType", value(patient.getBloodType()));
    view.put("heightCm", patient.getHeightCm() == null ? 0 : patient.getHeightCm());
    view.put("weightKg", patient.getWeightKg() == null ? 0 : patient.getWeightKg());
    view.put("updatedAt", patient.getUpdatedAt() == null ? "" : patient.getUpdatedAt().toString());
    return view;
  }

  private Map<String, Object> visitorView(PatientVisitor visitor) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", visitor.getId());
    view.put("visitorType", "VISITOR");
    view.put("editable", true);
    view.put("name", visitor.getName());
    view.put("relationship", value(visitor.getRelationship()));
    view.put("phone", value(visitor.getPhone()));
    view.put("gender", value(visitor.getGender()));
    view.put("age", visitor.getAge() == null ? 0 : visitor.getAge());
    view.put("address", value(visitor.getAddress()));
    view.put("emergencyContact", value(visitor.getEmergencyContact()));
    view.put("emergencyPhone", value(visitor.getEmergencyPhone()));
    view.put("bloodType", value(visitor.getBloodType()));
    view.put("heightCm", visitor.getHeightCm() == null ? 0 : visitor.getHeightCm());
    view.put("weightKg", visitor.getWeightKg() == null ? 0 : visitor.getWeightKg());
    view.put("allergyHistory", value(visitor.getAllergyHistory()));
    view.put("pastHistory", value(visitor.getPastHistory()));
    view.put("updatedAt", visitor.getUpdatedAt() == null ? "" : visitor.getUpdatedAt().toString());
    return view;
  }

  private String value(String text) {
    return text == null ? "" : text;
  }
}

