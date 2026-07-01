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
import com.smartcloudbrain.prescription.entity.Drug;
import com.smartcloudbrain.prescription.entity.Patient;
import com.smartcloudbrain.prescription.dto.prescription.PrescriptionCreateRequest;
import com.smartcloudbrain.prescription.entity.Prescription;
import com.smartcloudbrain.prescription.entity.PrescriptionCheckRecord;
import com.smartcloudbrain.prescription.entity.PrescriptionItem;
import com.smartcloudbrain.prescription.event.OutboxEventPublisher;
import com.smartcloudbrain.prescription.repository.PrescriptionCheckRecordRepository;
import com.smartcloudbrain.prescription.repository.DrugRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionItemRepository;
import com.smartcloudbrain.prescription.repository.PrescriptionRepository;
import com.smartcloudbrain.prescription.repository.MedicalRecordRepository;
import com.smartcloudbrain.prescription.repository.PatientRepository;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final DrugRepository drugRepository;
  private final OutboxEventPublisher outboxEventPublisher;
  private final CurrentUserService currentUserService;

  public PrescriptionService(
      AiGatewayService aiGatewayService,
      PrescriptionRepository prescriptionRepository,
      PrescriptionItemRepository prescriptionItemRepository,
      PrescriptionCheckRecordRepository checkRecordRepository,
      MedicalRecordRepository medicalRecordRepository,
      PatientRepository patientRepository,
      DrugRepository drugRepository,
      OutboxEventPublisher outboxEventPublisher,
      CurrentUserService currentUserService
  ) {
    this.aiGatewayService = aiGatewayService;
    this.prescriptionRepository = prescriptionRepository;
    this.prescriptionItemRepository = prescriptionItemRepository;
    this.checkRecordRepository = checkRecordRepository;
    this.medicalRecordRepository = medicalRecordRepository;
    this.patientRepository = patientRepository;
    this.drugRepository = drugRepository;
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
    List<DrugItem> requestedDrugs = request.drugs() == null ? List.of() : request.drugs();
    Map<String, Drug> catalogDrugs = catalogDrugs(requestedDrugs);
    List<DrugItem> enrichedDrugs = requestedDrugs.stream()
        .map(drug -> enrichDrugItem(drug, catalogDrugs.get(normalizeDrugName(drug.drugName()))))
        .toList();
    PrescriptionCheckResponse response = aiGatewayService.checkPrescription(new PrescriptionCheckRequest(
        request.patientId(),
        doctorId,
        medicalRecord == null ? request.medicalRecordId() : medicalRecord.getId(),
        medicalRecord == null ? request.diagnosis() : medicalRecord.getDiagnosis(),
        patient == null ? request.patientAge() : patient.getAge(),
        patient == null ? request.patientGender() : patient.getGender(),
        patient == null ? request.allergyHistory() : patient.getAllergyHistory(),
        combinedPastHistory(request.pastHistory(), patient, medicalRecord),
        enrichedDrugs
    ));
    PrescriptionCheckRecord record = new PrescriptionCheckRecord();
    record.setPatientId(request.patientId());
    record.setDoctorId(doctorId);
    record.setRiskLevel(response.riskLevel());
    record.setSuggestions(nullToEmpty(response.suggestions()));
    record.setInteractions(String.join(",", safeInteractions(response)));
    record.setAiResultJson("{\"degraded\":" + response.degraded()
        + ",\"provider\":\"" + nullToEmpty(response.provider())
        + "\",\"model\":\"" + nullToEmpty(response.model()) + "\"}");
    checkRecordRepository.save(record);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("riskLevel", response.riskLevel());
    result.put("riskDescription", nullToEmpty(response.riskDescription()));
    result.put("suggestions", nullToEmpty(response.suggestions()));
    result.put("interactions", safeInteractions(response));
    result.put("contraindications", response.contraindications() == null ? List.of() : response.contraindications());
    result.put("adjustmentSuggestions", response.adjustmentSuggestions() == null ? List.of() : response.adjustmentSuggestions());
    result.put("drugKnowledge", drugKnowledge(requestedDrugs, catalogDrugs));
    result.put("degraded", response.degraded());
    result.put("provider", nullToEmpty(response.provider()));
    result.put("model", nullToEmpty(response.model()));
    result.put("checkRecordId", record.getId());
    return result;
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
          "已确认的处方需要风险随访。",
          List.of(),
          false
      ));
    }
    publishPrescriptionAudit(saved, user);
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

  public List<Map<String, Object>> availableDrugs() {
    currentUserService.require(RoleType.DOCTOR);
    return drugRepository.findByStatusIgnoreCase("ENABLED").stream()
        .map(this::drugView)
        .toList();
  }

  private Map<String, Object> drugView(Drug drug) {
    return Map.of(
        "id", drug.getId() == null ? 0L : drug.getId(),
        "name", nullToEmpty(drug.getName()),
        "specification", nullToEmpty(drug.getSpecification()),
        "contraindication", nullToEmpty(drug.getContraindication()),
        "interactionRule", nullToEmpty(drug.getInteractionRule()),
        "status", nullToEmpty(drug.getStatus())
    );
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
    String patientName = "";
    if (prescription.getPatientId() != null) {
      patientName = patientRepository.findById(prescription.getPatientId())
          .map(Patient::getName)
          .orElse("");
    }
    return Map.of(
        "prescriptionId", prescription.getId(),
        "patientId", prescription.getPatientId(),
        "patientName", patientName == null ? "" : patientName,
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
    payload.put("title", "AI处方风险提醒");
    payload.put("content", nullToEmpty(response.suggestions()));
    return payload;
  }

  private void publishPrescriptionAudit(Prescription prescription, AuthenticatedUser user) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("actorType", user.role().name());
    payload.put("actorId", user.userId());
    payload.put("action", "PRESCRIPTION_CREATED");
    payload.put("resourceType", "PRESCRIPTION");
    payload.put("resourceId", String.valueOf(prescription.getId()));
    payload.put("outcome", "SUCCESS");
    payload.put("doctorId", prescription.getDoctorId());
    payload.put("patientId", prescription.getPatientId());
    payload.put("prescriptionId", prescription.getId());
    payload.put("medicalRecordId", prescription.getMedicalRecordId() == null ? 0L : prescription.getMedicalRecordId());
    payload.put("registrationId", prescription.getRegistrationId() == null ? 0L : prescription.getRegistrationId());
    payload.put("riskLevel", prescription.getRiskLevel() == null ? "" : prescription.getRiskLevel());
    outboxEventPublisher.enqueue(DomainEventNames.PRESCRIPTION_CREATED, RabbitTopology.ROUTING_AUDIT_LOG, payload);
  }

  private Map<String, Drug> catalogDrugs(List<DrugItem> drugs) {
    List<String> names = drugs.stream()
        .map(DrugItem::drugName)
        .map(this::normalizeDrugName)
        .filter(name -> !name.isBlank())
        .toList();
    if (names.isEmpty()) {
      return Map.of();
    }
    return drugRepository.findByStatusIgnoreCase("ENABLED").stream()
        .filter(drug -> names.contains(normalizeDrugName(drug.getName())))
        .collect(Collectors.toMap(
            drug -> normalizeDrugName(drug.getName()),
            Function.identity(),
            (first, ignored) -> first,
            LinkedHashMap::new
        ));
  }

  private DrugItem enrichDrugItem(DrugItem item, Drug catalogDrug) {
    if (catalogDrug == null) {
      return item;
    }
    return new DrugItem(
        item.drugName(),
        item.dosage(),
        item.frequency(),
        item.usageMethod(),
        item.days(),
        appendDrugKnowledge(item.remark(), catalogDrug)
    );
  }

  private String appendDrugKnowledge(String originalRemark, Drug drug) {
    List<String> details = new ArrayList<>();
    addDrugDetail(details, "specification", drug.getSpecification());
    addDrugDetail(details, "contraindication", drug.getContraindication());
    addDrugDetail(details, "interactionRule", drug.getInteractionRule());
    if (details.isEmpty()) {
      return nullToEmpty(originalRemark);
    }
    String drugKnowledge = "Drug catalog: " + String.join("; ", details);
    return originalRemark == null || originalRemark.isBlank()
        ? drugKnowledge
        : originalRemark + "\n" + drugKnowledge;
  }

  private void addDrugDetail(List<String> details, String label, String value) {
    if (value != null && !value.isBlank()) {
      details.add(label + "=" + value.trim());
    }
  }

  private List<Map<String, Object>> drugKnowledge(List<DrugItem> requestedDrugs, Map<String, Drug> catalogDrugs) {
    return requestedDrugs.stream()
        .map(drug -> catalogDrugs.get(normalizeDrugName(drug.drugName())))
        .filter(Objects::nonNull)
        .map(drug -> {
          Map<String, Object> payload = new LinkedHashMap<>();
          payload.put("name", nullToEmpty(drug.getName()));
          payload.put("specification", nullToEmpty(drug.getSpecification()));
          payload.put("contraindication", nullToEmpty(drug.getContraindication()));
          payload.put("interactionRule", nullToEmpty(drug.getInteractionRule()));
          return payload;
        })
        .toList();
  }

  private String normalizeDrugName(String name) {
    return name == null ? "" : name.trim().toLowerCase();
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
