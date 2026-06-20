package com.smartcloudbrain.prescription.service;

import com.smartcloudbrain.aiapi.dto.DrugItem;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckRequest;
import com.smartcloudbrain.aiapi.dto.PrescriptionCheckResponse;
import com.smartcloudbrain.common.event.DomainEventNames;
import com.smartcloudbrain.common.event.RabbitTopology;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import com.smartcloudbrain.prescription.entity.MedicalRecord;
import com.smartcloudbrain.prescription.entity.Patient;
import com.smartcloudbrain.prescription.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.prescription.entity.Prescription;
import com.smartcloudbrain.prescription.entity.PrescriptionCheckRecord;
import com.smartcloudbrain.prescription.entity.PrescriptionItem;
import com.smartcloudbrain.prescription.event.OutboxEventPublisher;
import com.smartcloudbrain.prescription.repository.PrescriptionCheckRecordRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionItemRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionRepository;
import com.smartcloudbrain.prescription.repository.MedicalRecordRepository;
import com.smartcloudbrain.prescription.repository.PatientRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionService {

  private final AiGatewayService aiGatewayService;
  private final PrescriptionRepository prescriptionRepository;
  private final PrescriptionItemRepository prescriptionItemRepository;
  private final PrescriptionCheckRecordRepository checkRecordRepository;
  private final MedicalRecordRepository medicalRecordRepository;
  private final PatientRepository patientRepository;
  private final OutboxEventPublisher outboxEventPublisher;
  private final CurrentUserService currentUserService;

  public PrescriptionService(
      AiGatewayService aiGatewayService,
      PrescriptionRepository prescriptionRepository,
      PrescriptionItemRepository prescriptionItemRepository,
      PrescriptionCheckRecordRepository checkRecordRepository,
      MedicalRecordRepository medicalRecordRepository,
      PatientRepository patientRepository,
      OutboxEventPublisher outboxEventPublisher,
      CurrentUserService currentUserService
  ) {
    this.aiGatewayService = aiGatewayService;
    this.prescriptionRepository = prescriptionRepository;
    this.prescriptionItemRepository = prescriptionItemRepository;
    this.checkRecordRepository = checkRecordRepository;
    this.medicalRecordRepository = medicalRecordRepository;
    this.patientRepository = patientRepository;
    this.outboxEventPublisher = outboxEventPublisher;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public Map<String, Object> check(PrescriptionCheckRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    Long doctorId = request.doctorId() == null ? user.userId() : request.doctorId();
    if (!doctorId.equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Patient patient = patientRepository.findById(request.patientId()).orElse(null);
    MedicalRecord medicalRecord = resolveMedicalRecord(request, doctorId);
    PrescriptionCheckResponse response = aiGatewayService.checkPrescription(new PrescriptionCheckRequest(
        request.patientId(),
        doctorId,
        medicalRecord == null ? request.medicalRecordId() : medicalRecord.getId(),
        medicalRecord == null ? request.diagnosis() : medicalRecord.getDiagnosis(),
        patient == null ? request.patientAge() : patient.getAge(),
        patient == null ? request.patientGender() : patient.getGender(),
        patient == null ? request.allergyHistory() : patient.getAllergyHistory(),
        combinedPastHistory(request.pastHistory(), patient, medicalRecord),
        request.drugs()
    ));
    PrescriptionCheckRecord record = new PrescriptionCheckRecord();
    record.setPatientId(request.patientId());
    record.setDoctorId(doctorId);
    record.setRiskLevel(response.riskLevel());
    record.setSuggestions(nullToEmpty(response.suggestions()));
    record.setInteractions(String.join(",", safeInteractions(response)));
    record.setAiResultJson("{\"degraded\":" + response.degraded() + "}");
    checkRecordRepository.save(record);
    return Map.of(
        "riskLevel", response.riskLevel(),
        "riskDescription", response.riskDescription(),
        "suggestions", response.suggestions(),
        "interactions", safeInteractions(response),
        "contraindications", response.contraindications() == null ? List.of() : response.contraindications(),
        "adjustmentSuggestions", response.adjustmentSuggestions() == null ? List.of() : response.adjustmentSuggestions(),
        "degraded", response.degraded(),
        "checkRecordId", record.getId()
    );
  }

  @Transactional
  public Map<String, Object> create(PrescriptionCreateRequest request) {
    AuthenticatedUser user = currentUserService.require(RoleType.DOCTOR);
    MedicalRecord medicalRecord = medicalRecordRepository.findById(request.medicalRecordId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!medicalRecord.getDoctorId().equals(user.userId()) || !medicalRecord.getPatientId().equals(request.patientId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Prescription prescription = new Prescription();
    prescription.setPatientId(request.patientId());
    prescription.setDoctorId(user.userId());
    prescription.setMedicalRecordId(request.medicalRecordId());
    prescription.setRegistrationId(medicalRecord.getRegistrationId());
    prescription.setRiskLevel(request.riskLevel() == null ? "UNREVIEWED" : request.riskLevel());
    prescription.setStatus("CONFIRMED");
    Prescription saved = prescriptionRepository.save(prescription);
    for (DrugItem drug : request.drugs()) {
      PrescriptionItem item = new PrescriptionItem();
      item.setPrescriptionId(saved.getId());
      item.setDrugName(drug.drugName());
      item.setDosage(drug.dosage());
      item.setFrequency(drug.frequency());
      item.setUsageMethod(drug.usageMethod());
      item.setDays(drug.days());
      item.setRemark(drug.remark());
      prescriptionItemRepository.save(item);
    }
    if ("HIGH".equalsIgnoreCase(saved.getRiskLevel()) || "MEDIUM".equalsIgnoreCase(saved.getRiskLevel())) {
      publishRiskNotification(user.userId(), request.patientId(), saved.getId(), new PrescriptionCheckResponse(
          saved.getRiskLevel(),
          "Confirmed prescription requires risk follow-up.",
          List.of(),
          false
      ));
    }
    return prescriptionView(saved);
  }

  public List<Map<String, Object>> list() {
    AuthenticatedUser user = currentUserService.get();
    List<Prescription> prescriptions;
    if (user.role() == RoleType.PATIENT) {
      prescriptions = prescriptionRepository.findByPatientId(user.userId());
    } else if (user.role() == RoleType.DOCTOR) {
      prescriptions = prescriptionRepository.findByDoctorId(user.userId());
    } else {
      prescriptions = prescriptionRepository.findAll();
    }
    return prescriptions.stream().map(this::prescriptionView).toList();
  }

  public Map<String, Object> detail(Long id) {
    Prescription prescription = prescriptionRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    AuthenticatedUser user = currentUserService.get();
    if (user.role() == RoleType.PATIENT && !prescription.getPatientId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (user.role() == RoleType.DOCTOR && !prescription.getDoctorId().equals(user.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return prescriptionView(prescription);
  }

  private Map<String, Object> prescriptionView(Prescription prescription) {
    List<Map<String, Object>> items = prescriptionItemRepository.findByPrescriptionId(prescription.getId()).stream()
        .map(item -> Map.<String, Object>of(
            "drugName", item.getDrugName(),
            "dosage", item.getDosage(),
            "frequency", item.getFrequency(),
            "usageMethod", item.getUsageMethod(),
            "days", item.getDays() == null ? 0 : item.getDays(),
            "remark", item.getRemark() == null ? "" : item.getRemark()
        ))
        .toList();
    return Map.of(
        "prescriptionId", prescription.getId(),
        "patientId", prescription.getPatientId(),
        "doctorId", prescription.getDoctorId(),
        "medicalRecordId", prescription.getMedicalRecordId() == null ? 0L : prescription.getMedicalRecordId(),
        "registrationId", prescription.getRegistrationId() == null ? 0L : prescription.getRegistrationId(),
        "riskLevel", prescription.getRiskLevel() == null ? "" : prescription.getRiskLevel(),
        "status", prescription.getStatus(),
        "items", items,
        "createdAt", prescription.getCreatedAt() == null ? "" : prescription.getCreatedAt().toString()
    );
  }

  private void publishRiskNotification(Long doctorId, Long patientId, Long prescriptionId, PrescriptionCheckResponse response) {
    outboxEventPublisher.enqueue(
        DomainEventNames.PRESCRIPTION_CHECKED,
        RabbitTopology.ROUTING_NOTIFICATION_DISPATCH,
        riskNotificationPayload(doctorId, patientId, prescriptionId, response)
    );
  }

  private Map<String, Object> riskNotificationPayload(
      Long doctorId,
      Long patientId,
      Long prescriptionId,
      PrescriptionCheckResponse response
  ) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("doctorId", doctorId);
    payload.put("patientId", patientId);
    payload.put("prescriptionId", prescriptionId == null ? 0L : prescriptionId);
    payload.put("riskLevel", nullToEmpty(response.riskLevel()));
    payload.put("suggestions", nullToEmpty(response.suggestions()));
    payload.put("interactions", safeInteractions(response));
    payload.put("type", "PRESCRIPTION_HIGH_RISK");
    payload.put("title", "AI prescription risk alert");
    return payload;
  }

  private List<String> safeInteractions(PrescriptionCheckResponse response) {
    return response.interactions() == null ? List.of() : response.interactions();
  }

  private MedicalRecord resolveMedicalRecord(PrescriptionCheckRequest request, Long doctorId) {
    if (request.medicalRecordId() != null) {
      MedicalRecord record = medicalRecordRepository.findById(request.medicalRecordId())
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
      if (!record.getDoctorId().equals(doctorId) || !record.getPatientId().equals(request.patientId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
      }
      return record;
    }
    return medicalRecordRepository.findFirstByPatientIdAndDoctorIdOrderByIdDesc(request.patientId(), doctorId).orElse(null);
  }

  private String combinedPastHistory(String requestPastHistory, Patient patient, MedicalRecord record) {
    StringBuilder builder = new StringBuilder();
    append(builder, requestPastHistory);
    append(builder, patient == null ? "" : patient.getPastHistory());
    append(builder, record == null ? "" : record.getPastHistory());
    return builder.toString();
  }

  private void append(StringBuilder builder, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!builder.isEmpty()) {
      builder.append("\n");
    }
    builder.append(value);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}


