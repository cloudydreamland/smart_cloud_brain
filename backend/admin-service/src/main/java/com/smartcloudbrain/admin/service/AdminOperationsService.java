package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.admin.client.InternalDoctorClient;
import com.smartcloudbrain.admin.dto.admin.DeviceSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceStatusRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceUsageSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientSaveRequest;
import com.smartcloudbrain.admin.dto.admin.RolePermissionSaveRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleCancelRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleSaveRequest;
import com.smartcloudbrain.admin.entity.DeviceUsageRecord;
import com.smartcloudbrain.admin.entity.MedicalDevice;
import com.smartcloudbrain.admin.entity.RolePermission;
import com.smartcloudbrain.admin.repository.DeviceUsageRecordRepository;
import com.smartcloudbrain.admin.repository.MedicalDeviceRepository;
import com.smartcloudbrain.admin.repository.RolePermissionRepository;
import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOperationsService {

  private static final List<Map<String, String>> PERMISSION_CATALOG = List.of(
      permission("dashboard:view", "Workspace", "View admin dashboard"),
      permission("department:manage", "Departments", "Manage departments"),
      permission("doctor:manage", "Doctors", "Manage doctors"),
      permission("drug:manage", "Drugs", "Manage drugs"),
      permission("schedule:manage", "Schedules", "Manage schedules and appointment slots"),
      permission("triage:manage", "Triage desk", "Assign and close triage records"),
      permission("device:manage", "Devices", "Manage medical devices and usage records"),
      permission("patient:manage", "Patients", "Manage patient profiles and history"),
      permission("statistics:view", "Statistics", "View operational statistics"),
      permission("statistics:export", "Report export", "Export statistics as CSV"),
      permission("account:manage", "Accounts", "Manage accounts"),
      permission("permission:manage", "Permissions", "Manage role permissions"),
      permission("knowledge:manage", "Knowledge", "Manage knowledge entries"),
      permission("prompt:manage", "Prompts", "Manage AI prompt templates"),
      permission("dict:manage", "Dictionaries", "Manage dictionaries"),
      permission("patient-site:manage", "Patient site", "Manage patient site navigation and content"),
      permission("search:view", "Search", "Use admin search")
  );

  private final MedicalDeviceRepository medicalDeviceRepository;
  private final DeviceUsageRecordRepository deviceUsageRecordRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final InternalDoctorClient internalDoctorClient;
  private final JdbcTemplate jdbcTemplate;

  public AdminOperationsService(
      MedicalDeviceRepository medicalDeviceRepository,
      DeviceUsageRecordRepository deviceUsageRecordRepository,
      RolePermissionRepository rolePermissionRepository,
      InternalDoctorClient internalDoctorClient,
      JdbcTemplate jdbcTemplate
  ) {
    this.medicalDeviceRepository = medicalDeviceRepository;
    this.deviceUsageRecordRepository = deviceUsageRecordRepository;
    this.rolePermissionRepository = rolePermissionRepository;
    this.internalDoctorClient = internalDoctorClient;
    this.jdbcTemplate = jdbcTemplate;
  }

  public boolean hasPermission(RoleType role, String permissionKey) {
    if (role == RoleType.ADMIN && rolePermissionRepository.countByRole(role.name()) == 0) {
      return true;
    }
    return rolePermissionRepository.findByRoleAndPermissionKey(role.name(), permissionKey)
        .map(permission -> Boolean.TRUE.equals(permission.getEnabled()))
        .orElse(role == RoleType.ADMIN && defaultAdminPermissions().contains(permissionKey));
  }

  public List<Map<String, Object>> devices(String keyword, Long departmentId, String category, String status) {
    String query = normalize(keyword);
    return medicalDeviceRepository.findAll().stream()
        .filter(device -> departmentId == null || departmentId.equals(device.getDepartmentId()))
        .filter(device -> isBlank(category) || category.equalsIgnoreCase(device.getCategory()))
        .filter(device -> isBlank(status) || status.equalsIgnoreCase(device.getStatus()))
        .filter(device -> query.isBlank()
            || contains(device.getDeviceCode(), query)
            || contains(device.getName(), query)
            || contains(device.getCategory(), query)
            || contains(device.getLocation(), query))
        .map(this::deviceView)
        .toList();
  }

  @Transactional
  public Map<String, Object> saveDevice(DeviceSaveRequest request) {
    MedicalDevice device = request.id() == null
        ? medicalDeviceRepository.findByDeviceCode(request.deviceCode()).orElseGet(MedicalDevice::new)
        : medicalDeviceRepository.findById(request.id()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    medicalDeviceRepository.findByDeviceCode(request.deviceCode()).ifPresent(existing -> {
      if (device.getId() != null && !device.getId().equals(existing.getId())) {
        throw new BusinessException(ErrorCode.CONFLICT);
      }
    });
    device.setDeviceCode(request.deviceCode());
    device.setName(request.name());
    device.setCategory(request.category());
    device.setDepartmentId(request.departmentId());
    device.setLocation(request.location());
    device.setStatus(isBlank(request.status()) ? "AVAILABLE" : request.status());
    device.setPurchaseDate(request.purchaseDate());
    device.setRemark(request.remark());
    device.setUpdatedAt(LocalDateTime.now());
    return deviceView(medicalDeviceRepository.save(device));
  }

  @Transactional
  public Map<String, Object> updateDeviceStatus(DeviceStatusRequest request) {
    MedicalDevice device = medicalDeviceRepository.findById(request.deviceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    device.setStatus(request.status());
    if ("MAINTENANCE".equalsIgnoreCase(request.status())) {
      device.setLastMaintenanceAt(LocalDateTime.now());
    }
    device.setUpdatedAt(LocalDateTime.now());
    return deviceView(medicalDeviceRepository.save(device));
  }

  public List<Map<String, Object>> deviceUsages(Long deviceId) {
    List<DeviceUsageRecord> source = deviceId == null
        ? deviceUsageRecordRepository.findAll()
        : deviceUsageRecordRepository.findByDeviceIdOrderByStartedAtDescIdDesc(deviceId);
    return source.stream().map(this::usageView).toList();
  }

  @Transactional
  public Map<String, Object> saveDeviceUsage(DeviceUsageSaveRequest request) {
    MedicalDevice device = medicalDeviceRepository.findById(request.deviceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    DeviceUsageRecord record = request.id() == null
        ? new DeviceUsageRecord()
        : deviceUsageRecordRepository.findById(request.id()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    record.setDeviceId(request.deviceId());
    record.setUsageType(request.usageType());
    record.setUsedBy(request.usedBy());
    record.setPatientId(request.patientId());
    record.setStartedAt(request.startedAt() == null ? LocalDateTime.now() : request.startedAt());
    record.setEndedAt(request.endedAt());
    record.setResultStatus(isBlank(request.resultStatus()) ? "NORMAL" : request.resultStatus());
    record.setRemark(request.remark());
    record.setUpdatedAt(LocalDateTime.now());
    if ("MAINTENANCE".equalsIgnoreCase(record.getUsageType())) {
      device.setLastMaintenanceAt(record.getStartedAt());
      medicalDeviceRepository.save(device);
    }
    return usageView(deviceUsageRecordRepository.save(record));
  }

  public List<Map<String, Object>> patients(String keyword, String gender, Integer minAge, Integer maxAge) {
    StringBuilder sql = new StringBuilder("SELECT p.*, COUNT(r.id) AS registration_count FROM patient p LEFT JOIN registration r ON r.patient_id = p.id WHERE 1=1");
    java.util.ArrayList<Object> params = new java.util.ArrayList<>();
    if (!isBlank(keyword)) {
      sql.append(" AND (LOWER(p.name) LIKE ? OR LOWER(p.phone) LIKE ?)");
      String value = "%" + normalize(keyword) + "%";
      params.add(value);
      params.add(value);
    }
    if (!isBlank(gender)) {
      sql.append(" AND p.gender = ?");
      params.add(gender);
    }
    if (minAge != null) {
      sql.append(" AND p.age >= ?");
      params.add(minAge);
    }
    if (maxAge != null) {
      sql.append(" AND p.age <= ?");
      params.add(maxAge);
    }
    sql.append(" GROUP BY p.id ORDER BY p.id DESC");
    return jdbcTemplate.queryForList(sql.toString(), params.toArray()).stream().map(this::patientView).toList();
  }

  public Map<String, Object> patientDetail(Long patientId) {
    Map<String, Object> patient = jdbcTemplate.queryForList("SELECT * FROM patient WHERE id = ?", patientId).stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    Map<String, Object> view = new LinkedHashMap<>(patientView(patient));
    view.put("registrations", jdbcTemplate.queryForList("SELECT * FROM registration WHERE patient_id = ? ORDER BY appointment_time DESC NULLS LAST, id DESC", patientId));
    view.put("triageRecords", jdbcTemplate.queryForList("SELECT * FROM triage_record WHERE patient_id = ? ORDER BY created_at DESC, id DESC", patientId));
    view.put("medicalRecords", jdbcTemplate.queryForList("SELECT * FROM medical_record WHERE patient_id = ? ORDER BY created_at DESC, id DESC", patientId));
    view.put("prescriptions", jdbcTemplate.queryForList("SELECT * FROM prescription WHERE patient_id = ? ORDER BY created_at DESC, id DESC", patientId));
    return view;
  }

  @Transactional
  public Map<String, Object> savePatient(PatientSaveRequest request) {
    int changed = jdbcTemplate.update("""
        UPDATE patient
        SET name = ?, gender = ?, age = ?, allergy_history = ?, past_history = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """, request.name(), request.gender(), request.age(), request.allergyHistory(), request.pastHistory(), request.id());
    if (changed == 0) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    return patientDetail(request.id());
  }

  public Map<String, Object> overview() {
    return Map.of(
        "registrations", count("SELECT COUNT(*) FROM registration"),
        "completedRegistrations", count("SELECT COUNT(*) FROM registration WHERE status = 'COMPLETED'"),
        "patients", count("SELECT COUNT(*) FROM patient"),
        "doctors", count("SELECT COUNT(*) FROM doctor WHERE COALESCE(status, 'ENABLED') <> 'DISABLED'"),
        "devices", count("SELECT COUNT(*) FROM medical_device WHERE status <> 'RETIRED'"),
        "deviceWarnings", count("SELECT COUNT(*) FROM device_usage_record WHERE result_status <> 'NORMAL'")
    );
  }

  public List<Map<String, Object>> trend(String startDate, String endDate) {
    String start = blankToNullString(startDate);
    String end = blankToNullString(endDate);
    StringBuilder sql = new StringBuilder("""
        SELECT CAST(appointment_time AS DATE) AS day, COUNT(*) AS registrations
        FROM registration
        WHERE appointment_time IS NOT NULL
        """);
    java.util.ArrayList<Object> params = new java.util.ArrayList<>();
    if (start != null) {
      sql.append(" AND appointment_time >= CAST(? AS TIMESTAMP)");
      params.add(start);
    }
    if (end != null) {
      sql.append(" AND appointment_time < CAST(? AS TIMESTAMP) + INTERVAL '1 day'");
      params.add(end);
    }
    sql.append("""
        GROUP BY CAST(appointment_time AS DATE)
        ORDER BY day
        """);
    return jdbcTemplate.queryForList(sql.toString(), params.toArray());
  }

  public List<Map<String, Object>> doctorWorkload(String startDate, String endDate) {
    String start = blankToNullString(startDate);
    String end = blankToNullString(endDate);
    StringBuilder sql = new StringBuilder("""
        SELECT d.id AS doctor_id, d.name AS doctor_name, dep.name AS department_name,
               COUNT(r.id) AS registrations,
               SUM(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
        FROM doctor d
        LEFT JOIN department dep ON dep.id = d.department_id
        LEFT JOIN registration r ON r.doctor_id = d.id
        """);
    java.util.ArrayList<Object> params = new java.util.ArrayList<>();
    if (start != null) {
      sql.append(" AND r.appointment_time >= CAST(? AS TIMESTAMP)");
      params.add(start);
    }
    if (end != null) {
      sql.append(" AND r.appointment_time < CAST(? AS TIMESTAMP) + INTERVAL '1 day'");
      params.add(end);
    }
    sql.append("""
        GROUP BY d.id, d.name, dep.name
        ORDER BY registrations DESC, d.id ASC
        """);
    return jdbcTemplate.queryForList(sql.toString(), params.toArray());
  }

  public Map<String, Object> patientDistribution() {
    return Map.of(
        "gender", jdbcTemplate.queryForList("SELECT COALESCE(gender, 'UNKNOWN') AS name, COUNT(*) AS value FROM patient GROUP BY COALESCE(gender, 'UNKNOWN')"),
        "age", jdbcTemplate.queryForList("""
            SELECT bucket AS name, COUNT(*) AS value FROM (
              SELECT CASE
                WHEN age IS NULL THEN 'UNKNOWN'
                WHEN age < 18 THEN '0-17'
                WHEN age < 40 THEN '18-39'
                WHEN age < 60 THEN '40-59'
                ELSE '60+'
              END AS bucket
              FROM patient
            ) t GROUP BY bucket ORDER BY bucket
            """)
    );
  }

  public List<Map<String, Object>> deviceUsage() {
    return jdbcTemplate.queryForList("""
        SELECT d.id AS device_id, d.name, d.device_code, d.category, d.status,
               COUNT(u.id) AS usage_count,
               SUM(CASE WHEN u.result_status <> 'NORMAL' THEN 1 ELSE 0 END) AS abnormal_count
        FROM medical_device d
        LEFT JOIN device_usage_record u ON u.device_id = d.id
        GROUP BY d.id, d.name, d.device_code, d.category, d.status
        ORDER BY usage_count DESC, d.id ASC
        """);
  }

  public List<Map<String, Object>> report(String startDate, String endDate) {
    java.util.ArrayList<Map<String, Object>> rows = new java.util.ArrayList<>();
    rows.add(metric("registrations", count("SELECT COUNT(*) FROM registration")));
    rows.add(metric("completed", count("SELECT COUNT(*) FROM registration WHERE status = 'COMPLETED'")));
    rows.add(metric("patients", count("SELECT COUNT(*) FROM patient")));
    rows.add(metric("devices", count("SELECT COUNT(*) FROM medical_device WHERE status <> 'RETIRED'")));
    rows.add(metric("device_abnormal", count("SELECT COUNT(*) FROM device_usage_record WHERE result_status <> 'NORMAL'")));
    return rows;
  }

  public List<Map<String, Object>> schedules(String startDate, String endDate, Long departmentId, Long doctorId, String status) {
    return internalDoctorClient.schedules(startDate, endDate, departmentId, doctorId, status);
  }

  public Map<String, Object> saveSchedule(ScheduleSaveRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (request.id() != null) {
      body.put("id", request.id());
    }
    body.put("doctorId", request.doctorId());
    body.put("departmentId", request.departmentId());
    body.put("workDate", request.workDate().toString());
    body.put("timeRange", request.timeRange());
    body.put("capacity", request.capacity());
    body.put("status", isBlank(request.status()) ? "PUBLISHED" : request.status());
    return internalDoctorClient.saveSchedule(body);
  }

  public Map<String, Object> cancelSchedule(ScheduleCancelRequest request) {
    return internalDoctorClient.cancelSchedule(request.scheduleId());
  }

  public Map<String, Object> permissions() {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("catalog", PERMISSION_CATALOG);
    view.put("roles", List.of(RoleType.ADMIN.name(), RoleType.DOCTOR.name(), RoleType.PATIENT.name()));
    view.put("grants", rolePermissionRepository.findAll().stream().map(this::permissionView).toList());
    return view;
  }

  public List<String> myPermissions(RoleType role) {
    if (role == RoleType.ADMIN && rolePermissionRepository.countByRole(role.name()) == 0) {
      return defaultAdminPermissions().stream().sorted().toList();
    }
    return rolePermissionRepository.findByRoleAndEnabledTrueOrderByPermissionKeyAsc(role.name()).stream()
        .map(RolePermission::getPermissionKey)
        .toList();
  }

  @Transactional
  public Map<String, Object> saveRolePermissions(RolePermissionSaveRequest request) {
    RoleType role = parseRole(request.role());
    Set<String> requested = request.permissionKeys() == null ? Set.of() : Set.copyOf(request.permissionKeys());
    Set<String> allowed = PERMISSION_CATALOG.stream()
        .map(item -> item.get("key"))
        .collect(java.util.stream.Collectors.toSet());
    for (String key : requested) {
      if (!allowed.contains(key)) {
        throw new BusinessException(400, "Unsupported permission: " + key);
      }
    }
    for (Map<String, String> item : PERMISSION_CATALOG) {
      String key = item.get("key");
      RolePermission permission = rolePermissionRepository.findByRoleAndPermissionKey(role.name(), key).orElseGet(RolePermission::new);
      permission.setRole(role.name());
      permission.setPermissionKey(key);
      permission.setEnabled(requested.contains(key));
      permission.setUpdatedAt(LocalDateTime.now());
      rolePermissionRepository.save(permission);
    }
    return permissions();
  }

  private Map<String, Object> deviceView(MedicalDevice device) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", device.getId());
    view.put("deviceCode", value(device.getDeviceCode()));
    view.put("name", value(device.getName()));
    view.put("category", value(device.getCategory()));
    view.put("departmentId", device.getDepartmentId() == null ? 0L : device.getDepartmentId());
    view.put("departmentName", departmentName(device.getDepartmentId()));
    view.put("location", value(device.getLocation()));
    view.put("status", value(device.getStatus(), "AVAILABLE"));
    view.put("purchaseDate", device.getPurchaseDate() == null ? "" : device.getPurchaseDate().toString());
    view.put("lastMaintenanceAt", device.getLastMaintenanceAt() == null ? "" : device.getLastMaintenanceAt().toString());
    view.put("remark", value(device.getRemark()));
    view.put("usageCount", count("SELECT COUNT(*) FROM device_usage_record WHERE device_id = " + device.getId()));
    view.put("abnormalCount", count("SELECT COUNT(*) FROM device_usage_record WHERE device_id = " + device.getId() + " AND result_status <> 'NORMAL'"));
    return view;
  }

  private Map<String, Object> usageView(DeviceUsageRecord record) {
    return Map.of(
        "id", record.getId(),
        "deviceId", record.getDeviceId(),
        "usageType", value(record.getUsageType()),
        "usedBy", value(record.getUsedBy()),
        "patientId", record.getPatientId() == null ? 0L : record.getPatientId(),
        "startedAt", record.getStartedAt() == null ? "" : record.getStartedAt().toString(),
        "endedAt", record.getEndedAt() == null ? "" : record.getEndedAt().toString(),
        "resultStatus", value(record.getResultStatus(), "NORMAL"),
        "remark", value(record.getRemark())
    );
  }

  private Map<String, Object> patientView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("name", value(row.get("name")));
    view.put("phone", value(row.get("phone")));
    view.put("gender", value(row.get("gender")));
    view.put("age", row.get("age") == null ? 0 : row.get("age"));
    view.put("allergyHistory", value(row.get("allergy_history")));
    view.put("pastHistory", value(row.get("past_history")));
    view.put("registrationCount", row.get("registration_count") == null ? 0 : row.get("registration_count"));
    return view;
  }

  private Map<String, Object> permissionView(RolePermission permission) {
    return Map.of(
        "role", permission.getRole(),
        "permissionKey", permission.getPermissionKey(),
        "enabled", Boolean.TRUE.equals(permission.getEnabled())
    );
  }

  private String departmentName(Long departmentId) {
    if (departmentId == null) {
      return "";
    }
    return jdbcTemplate.queryForList("SELECT name FROM department WHERE id = ?", String.class, departmentId).stream().findFirst().orElse("");
  }

  private long count(String sql) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class);
    return value == null ? 0L : value;
  }

  private Map<String, Object> metric(String name, Object value) {
    return Map.of("metric", name, "value", value);
  }

  private static Map<String, String> permission(String key, String label, String description) {
    return Map.of("key", key, "label", label, "description", description);
  }

  private Set<String> defaultAdminPermissions() {
    return PERMISSION_CATALOG.stream().map(item -> item.get("key")).collect(java.util.stream.Collectors.toSet());
  }

  private RoleType parseRole(String role) {
    try {
      return RoleType.valueOf(normalize(role).toUpperCase(Locale.ROOT));
    } catch (Exception ex) {
      throw new BusinessException(400, "Unsupported role: " + role);
    }
  }

  private boolean contains(String value, String query) {
    return normalize(value).contains(query);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Object blankToNull(String value) {
    return isBlank(value) ? null : value;
  }

  private String blankToNullString(String value) {
    return isBlank(value) ? null : value;
  }

  private String value(Object value) {
    return value(value, "");
  }

  private String value(Object value, String fallback) {
    return value == null ? fallback : String.valueOf(value);
  }
}
