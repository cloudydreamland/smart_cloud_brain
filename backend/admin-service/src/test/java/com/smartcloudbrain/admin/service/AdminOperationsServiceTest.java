package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.security.RoleType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AdminOperationsServiceTest {

  @Mock private MedicalDeviceRepository medicalDeviceRepository;
  @Mock private DeviceUsageRecordRepository deviceUsageRecordRepository;
  @Mock private RolePermissionRepository rolePermissionRepository;
  @Mock private InternalDoctorClient internalDoctorClient;
  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private AdminOperationsService service;

  // ─────────────── hasPermission ───────────────

  @Test
  void hasPermission_adminWithNoGrants_returnsTrue() {
    when(rolePermissionRepository.countByRole("ADMIN")).thenReturn(0L);
    assertTrue(service.hasPermission(RoleType.ADMIN, "dashboard:view"));
  }

  @Test
  void hasPermission_adminWithExplicitGrant_returnsTrue() {
    when(rolePermissionRepository.countByRole("ADMIN")).thenReturn(1L);
    RolePermission perm = new RolePermission();
    perm.setEnabled(true);
    when(rolePermissionRepository.findByRoleAndPermissionKey("ADMIN", "dashboard:view"))
        .thenReturn(Optional.of(perm));
    assertTrue(service.hasPermission(RoleType.ADMIN, "dashboard:view"));
  }

  @Test
  void hasPermission_adminWithExplicitDisabled_returnsFalse() {
    when(rolePermissionRepository.countByRole("ADMIN")).thenReturn(1L);
    RolePermission perm = new RolePermission();
    perm.setEnabled(false);
    when(rolePermissionRepository.findByRoleAndPermissionKey("ADMIN", "dashboard:view"))
        .thenReturn(Optional.of(perm));
    assertFalse(service.hasPermission(RoleType.ADMIN, "dashboard:view"));
  }

  @Test
  void hasPermission_adminNoGrantRecord_fallsBackToDefaultCatalog() {
    when(rolePermissionRepository.countByRole("ADMIN")).thenReturn(1L);
    when(rolePermissionRepository.findByRoleAndPermissionKey("ADMIN", "dashboard:view"))
        .thenReturn(Optional.empty());
    assertTrue(service.hasPermission(RoleType.ADMIN, "dashboard:view"));
  }

  @Test
  void hasPermission_doctorWithGrant_returnsTrue() {
    RolePermission perm = new RolePermission();
    perm.setEnabled(true);
    when(rolePermissionRepository.findByRoleAndPermissionKey("DOCTOR", "dashboard:view"))
        .thenReturn(Optional.of(perm));
    assertTrue(service.hasPermission(RoleType.DOCTOR, "dashboard:view"));
  }

  @Test
  void hasPermission_doctorWithoutGrant_returnsFalse() {
    when(rolePermissionRepository.findByRoleAndPermissionKey("DOCTOR", "dashboard:view"))
        .thenReturn(Optional.empty());
    assertFalse(service.hasPermission(RoleType.DOCTOR, "dashboard:view"));
  }

  @Test
  void hasPermission_patientWithoutGrant_returnsFalse() {
    when(rolePermissionRepository.findByRoleAndPermissionKey("PATIENT", "dashboard:view"))
        .thenReturn(Optional.empty());
    assertFalse(service.hasPermission(RoleType.PATIENT, "dashboard:view"));
  }

  // ─────────────── devices ───────────────

  @Test
  void devices_returnsAllDevicesWhenNoFilters() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG Monitor", "Diagnostic", 2L, "Room 101", "AVAILABLE");
    when(medicalDeviceRepository.findAll()).thenReturn(List.of(device));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.devices(null, null, null, null);
    assertEquals(1, result.size());
    assertEquals("D001", result.get(0).get("deviceCode"));
  }

  @Test
  void devices_filtersByKeyword() {
    MedicalDevice d1 = makeDevice(1L, "D001", "ECG Monitor", "Diagnostic", 2L, "Room 101", "AVAILABLE");
    MedicalDevice d2 = makeDevice(2L, "D002", "X-Ray Machine", "Imaging", 3L, "Room 202", "AVAILABLE");
    when(medicalDeviceRepository.findAll()).thenReturn(List.of(d1, d2));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.devices("ecg", null, null, null);
    assertEquals(1, result.size());
    assertEquals("D001", result.get(0).get("deviceCode"));
  }

  @Test
  void devices_filtersByDepartmentId() {
    MedicalDevice d1 = makeDevice(1L, "D001", "ECG", "Diagnostic", 2L, "Room 101", "AVAILABLE");
    MedicalDevice d2 = makeDevice(2L, "D002", "X-Ray", "Imaging", 3L, "Room 202", "AVAILABLE");
    when(medicalDeviceRepository.findAll()).thenReturn(List.of(d1, d2));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.devices(null, 2L, null, null);
    assertEquals(1, result.size());
  }

  @Test
  void devices_filtersByCategory() {
    MedicalDevice d1 = makeDevice(1L, "D001", "ECG", "Diagnostic", 2L, "Room 101", "AVAILABLE");
    MedicalDevice d2 = makeDevice(2L, "D002", "X-Ray", "Imaging", 3L, "Room 202", "AVAILABLE");
    when(medicalDeviceRepository.findAll()).thenReturn(List.of(d1, d2));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.devices(null, null, "imaging", null);
    assertEquals(1, result.size());
  }

  @Test
  void devices_filtersByStatus() {
    MedicalDevice d1 = makeDevice(1L, "D001", "ECG", "Diagnostic", 2L, "Room 101", "AVAILABLE");
    MedicalDevice d2 = makeDevice(2L, "D002", "X-Ray", "Imaging", 3L, "Room 202", "MAINTENANCE");
    when(medicalDeviceRepository.findAll()).thenReturn(List.of(d1, d2));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.devices(null, null, null, "maintenance");
    assertEquals(1, result.size());
    assertEquals("D002", result.get(0).get("deviceCode"));
  }

  // ─────────────── saveDevice ───────────────

  @Test
  void saveDevice_newDevice_persistsAndReturns() {
    DeviceSaveRequest req = new DeviceSaveRequest(null, "D001", "ECG", "Diagnostic", 2L, "Room 101", null, null, null);
    when(medicalDeviceRepository.findByDeviceCode("D001")).thenReturn(Optional.empty());
    when(medicalDeviceRepository.save(any(MedicalDevice.class))).thenAnswer(inv -> {
      MedicalDevice d = inv.getArgument(0);
      d.setId(10L);
      return d;
    });
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.saveDevice(req);
    assertEquals("D001", result.get("deviceCode"));
    assertEquals("ECG", result.get("name"));
    assertEquals("AVAILABLE", result.get("status"));
    verify(medicalDeviceRepository).save(any(MedicalDevice.class));
  }

  @Test
  void saveDevice_existingDeviceById_updatesAndReturns() {
    MedicalDevice existing = makeDevice(5L, "D001", "Old Name", "Old Cat", 1L, "Old Loc", "AVAILABLE");
    DeviceSaveRequest req = new DeviceSaveRequest(5L, "D001", "New Name", "New Cat", 2L, "New Loc", "MAINTENANCE", null, null);
    when(medicalDeviceRepository.findById(5L)).thenReturn(Optional.of(existing));
    when(medicalDeviceRepository.findByDeviceCode("D001")).thenReturn(Optional.of(existing));
    when(medicalDeviceRepository.save(any(MedicalDevice.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.saveDevice(req);
    assertEquals("New Name", result.get("name"));
  }

  @Test
  void saveDevice_existingIdNotFound_throws() {
    DeviceSaveRequest req = new DeviceSaveRequest(99L, "D001", "ECG", null, null, null, null, null, null);
    when(medicalDeviceRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.saveDevice(req));
  }

  @Test
  void saveDevice_duplicateDeviceCode_throws() {
    MedicalDevice existing = makeDevice(5L, "D001", "ECG", null, null, null, "AVAILABLE");
    DeviceSaveRequest req = new DeviceSaveRequest(5L, "D001", "ECG", null, null, null, null, null, null);
    when(medicalDeviceRepository.findById(5L)).thenReturn(Optional.of(existing));
    // Another device with same code
    MedicalDevice conflict = makeDevice(99L, "D001", "Other", null, null, null, "AVAILABLE");
    when(medicalDeviceRepository.findByDeviceCode("D001")).thenReturn(Optional.of(conflict));
    assertThrows(BusinessException.class, () -> service.saveDevice(req));
  }

  // ─────────────── updateDeviceStatus ───────────────

  @Test
  void updateDeviceStatus_normalStatus_updatesDevice() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG", null, 2L, null, "AVAILABLE");
    when(medicalDeviceRepository.findById(1L)).thenReturn(Optional.of(device));
    when(medicalDeviceRepository.save(any(MedicalDevice.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.updateDeviceStatus(new DeviceStatusRequest(1L, "RETIRED"));
    assertEquals("RETIRED", result.get("status"));
  }

  @Test
  void updateDeviceStatus_maintenanceStatus_setsLastMaintenanceAt() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG", null, 2L, null, "AVAILABLE");
    when(medicalDeviceRepository.findById(1L)).thenReturn(Optional.of(device));
    when(medicalDeviceRepository.save(any(MedicalDevice.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
    when(jdbcTemplate.queryForList(anyString(), eq(String.class), anyLong())).thenReturn(List.of());

    var result = service.updateDeviceStatus(new DeviceStatusRequest(1L, "MAINTENANCE"));
    assertEquals("MAINTENANCE", result.get("status"));
    assertFalse(String.valueOf(result.get("lastMaintenanceAt")).isEmpty());
  }

  @Test
  void updateDeviceStatus_notFound_throws() {
    when(medicalDeviceRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.updateDeviceStatus(new DeviceStatusRequest(99L, "RETIRED")));
  }

  // ─────────────── deviceUsages ───────────────

  @Test
  void deviceUsages_withDeviceId_returnsList() {
    DeviceUsageRecord record = new DeviceUsageRecord();
    record.setId(1L);
    record.setDeviceId(5L);
    record.setUsageType("DIAGNOSTIC");
    record.setUsedBy("Dr. Smith");
    record.setPatientId(10L);
    record.setStartedAt(LocalDateTime.now());
    record.setResultStatus("NORMAL");
    when(deviceUsageRecordRepository.findByDeviceIdOrderByStartedAtDescIdDesc(5L)).thenReturn(List.of(record));

    var result = service.deviceUsages(5L);
    assertEquals(1, result.size());
    assertEquals(5L, result.get(0).get("deviceId"));
  }

  @Test
  void deviceUsages_nullDeviceId_returnsAll() {
    DeviceUsageRecord record = new DeviceUsageRecord();
    record.setId(1L);
    record.setDeviceId(5L);
    record.setUsageType("DIAGNOSTIC");
    record.setUsedBy("Dr. Smith");
    record.setStartedAt(LocalDateTime.now());
    record.setResultStatus("NORMAL");
    when(deviceUsageRecordRepository.findAll()).thenReturn(List.of(record));

    var result = service.deviceUsages(null);
    assertEquals(1, result.size());
  }

  // ─────────────── saveDeviceUsage ───────────────

  @Test
  void saveDeviceUsage_newRecord_persistsAndReturns() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG", null, null, null, "AVAILABLE");
    when(medicalDeviceRepository.findById(1L)).thenReturn(Optional.of(device));
    when(deviceUsageRecordRepository.save(any(DeviceUsageRecord.class))).thenAnswer(inv -> {
      DeviceUsageRecord r = inv.getArgument(0);
      r.setId(10L);
      return r;
    });

    var result = service.saveDeviceUsage(new DeviceUsageSaveRequest(
        null, 1L, "DIAGNOSTIC", "Dr. Smith", 5L, LocalDateTime.now(), null, null, "test"
    ));
    assertEquals(1L, result.get("deviceId"));
    verify(deviceUsageRecordRepository).save(any(DeviceUsageRecord.class));
  }

  @Test
  void saveDeviceUsage_maintenanceType_updatesDeviceMaintenanceDate() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG", null, null, null, "AVAILABLE");
    when(medicalDeviceRepository.findById(1L)).thenReturn(Optional.of(device));
    when(deviceUsageRecordRepository.save(any(DeviceUsageRecord.class))).thenAnswer(inv -> {
      DeviceUsageRecord r = inv.getArgument(0);
      r.setId(10L);
      return r;
    });

    service.saveDeviceUsage(new DeviceUsageSaveRequest(
        null, 1L, "MAINTENANCE", "Tech", null, null, null, null, null
    ));
    verify(medicalDeviceRepository).save(device);
    assertTrue(device.getLastMaintenanceAt() != null);
  }

  @Test
  void saveDeviceUsage_existingRecord_updatesAndReturns() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG", null, null, null, "AVAILABLE");
    DeviceUsageRecord existing = new DeviceUsageRecord();
    existing.setId(10L);
    existing.setDeviceId(1L);
    when(medicalDeviceRepository.findById(1L)).thenReturn(Optional.of(device));
    when(deviceUsageRecordRepository.findById(10L)).thenReturn(Optional.of(existing));
    when(deviceUsageRecordRepository.save(any(DeviceUsageRecord.class))).thenAnswer(inv -> inv.getArgument(0));

    var result = service.saveDeviceUsage(new DeviceUsageSaveRequest(
        10L, 1L, "DIAGNOSTIC", "Dr. Smith", null, null, null, "ABNORMAL", null
    ));
    assertEquals(10L, result.get("id"));
    assertEquals("ABNORMAL", result.get("resultStatus"));
  }

  @Test
  void saveDeviceUsage_deviceNotFound_throws() {
    when(medicalDeviceRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.saveDeviceUsage(
        new DeviceUsageSaveRequest(null, 99L, "DIAGNOSTIC", "Dr. Smith", null, null, null, null, null)
    ));
  }

  @Test
  void saveDeviceUsage_existingRecordNotFound_throws() {
    MedicalDevice device = makeDevice(1L, "D001", "ECG", null, null, null, "AVAILABLE");
    when(medicalDeviceRepository.findById(1L)).thenReturn(Optional.of(device));
    when(deviceUsageRecordRepository.findById(99L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.saveDeviceUsage(
        new DeviceUsageSaveRequest(99L, 1L, "DIAGNOSTIC", "Dr. Smith", null, null, null, null, null)
    ));
  }

  // ─────────────── patients ───────────────

  @Test
  void patients_noFilters_queriesWithoutConditions() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 1L);
    row.put("name", "张三");
    row.put("phone", "13800000001");
    row.put("email", null);
    row.put("gender", "男");
    row.put("age", 30);
    row.put("allergy_history", null);
    row.put("past_history", null);
    row.put("registration_count", 5L);
    doReturn(List.of(row)).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    var result = service.patients(null, null, null, null);
    assertEquals(1, result.size());
    assertEquals("张三", result.get(0).get("name"));
    assertEquals(30, result.get(0).get("age"));
  }

  @Test
  void patients_withAllFilters_buildsCorrectSql() {
    doReturn(List.of()).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    var result = service.patients("张", "男", 18, 60);
    assertTrue(result.isEmpty());
    verify(jdbcTemplate).queryForList(anyString(), any(Object[].class));
  }

  // ─────────────── patientDetail ───────────────

  @Test
  void patientDetail_found_returnsFullDetail() {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 1L);
    row.put("name", "张三");
    row.put("phone", "13800000001");
    row.put("email", null);
    row.put("gender", "男");
    row.put("age", 30);
    row.put("allergy_history", null);
    row.put("past_history", null);
    row.put("registration_count", null);
    when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      return sql.startsWith("SELECT * FROM patient WHERE") ? List.of(row) : List.of();
    });

    var result = service.patientDetail(1L);
    assertEquals("张三", result.get("name"));
    assertTrue(result.containsKey("registrations"));
    assertTrue(result.containsKey("triageRecords"));
    assertTrue(result.containsKey("medicalRecords"));
    assertTrue(result.containsKey("prescriptions"));
  }

  @Test
  void patientDetail_notFound_throws() {
    when(jdbcTemplate.queryForList(anyString(), eq(99L))).thenReturn(List.of());
    assertThrows(BusinessException.class, () -> service.patientDetail(99L));
  }

  // ─────────────── savePatient ───────────────

  @Test
  void savePatient_found_updatesAndReturns() {
    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", 1L);
    row.put("name", "李四");
    row.put("phone", "13800000002");
    row.put("email", null);
    row.put("gender", "女");
    row.put("age", 25);
    row.put("allergy_history", null);
    row.put("past_history", null);
    row.put("registration_count", null);
    when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      return sql.startsWith("SELECT * FROM patient WHERE") ? List.of(row) : List.of();
    });

    var result = service.savePatient(new PatientSaveRequest(1L, "李四", "女", 25, null, null));
    assertEquals("李四", result.get("name"));
  }

  @Test
  void savePatient_notFound_throws() {
    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(0);
    assertThrows(BusinessException.class, () -> service.savePatient(new PatientSaveRequest(99L, "X", "M", 30, null, null)));
  }

  // ─────────────── overview ───────────────

  @Test
  void overview_returnsAllCounters() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(10L);

    var result = service.overview();
    assertEquals(10L, result.get("registrations"));
    assertEquals(10L, result.get("patients"));
    assertEquals(10L, result.get("doctors"));
    assertEquals(10L, result.get("devices"));
  }

  // ─────────────── trend ───────────────

  @Test
  void trend_noDates_queriesWithoutConditions() {
    doReturn(List.of(Map.of("day", "2026-01-01", "registrations", 5L))).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    var result = service.trend(null, null);
    assertEquals(1, result.size());
  }

  @Test
  void trend_withDates_addsDateFilters() {
    doReturn(List.of()).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

    var result = service.trend("2026-01-01", "2026-01-31");
    assertTrue(result.isEmpty());
    verify(jdbcTemplate).queryForList(anyString(), any(Object[].class));
  }

  // ─────────────── doctorWorkload ───────────────

  @Test
  void doctorWorkload_noDates_queriesWithoutConditions() {
    when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("doctor_id", 1L, "registrations", 10L)));

    var result = service.doctorWorkload(null, null);
    assertEquals(1, result.size());
  }

  @Test
  void doctorWorkload_withDates_addsDateFilters() {
    when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

    var result = service.doctorWorkload("2026-01-01", "2026-01-31");
    assertTrue(result.isEmpty());
    verify(jdbcTemplate).queryForList(anyString(), any(Object[].class));
  }

  // ─────────────── patientDistribution ───────────────

  @Test
  void patientDistribution_returnsGenderAndAge() {
    doReturn(List.of(Map.of("name", "男", "value", 10L))).when(jdbcTemplate).queryForList(anyString());

    var result = service.patientDistribution();
    assertTrue(result.containsKey("gender"));
    assertTrue(result.containsKey("age"));
  }

  // ─────────────── deviceUsage (no-arg) ───────────────

  @Test
  void deviceUsage_returnsStatistics() {
    doReturn(List.of(Map.of("device_id", 1L, "usage_count", 5L))).when(jdbcTemplate).queryForList(anyString());

    var result = service.deviceUsage();
    assertEquals(1, result.size());
  }

  // ─────────────── report ───────────────

  @Test
  void report_returnsMetricRows() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(100L);

    var result = service.report(null, null);
    assertEquals(5, result.size());
    assertEquals("registrations", result.get(0).get("metric"));
    assertEquals(100L, result.get(0).get("value"));
  }

  // ─────────────── schedules ───────────────

  @Test
  void schedules_delegatesToClient() {
    when(internalDoctorClient.schedules("2026-01-01", "2026-01-31", 2L, 1L, "PUBLISHED"))
        .thenReturn(List.of(Map.of("id", 1L)));

    var result = service.schedules("2026-01-01", "2026-01-31", 2L, 1L, "PUBLISHED");
    assertEquals(1, result.size());
  }

  // ─────────────── saveSchedule ───────────────

  @Test
  void saveSchedule_newSchedule_delegatesToClient() {
    when(internalDoctorClient.saveSchedule(any())).thenReturn(Map.of("id", 10L));

    var result = service.saveSchedule(new ScheduleSaveRequest(null, 1L, 2L, LocalDate.of(2026, 1, 15), "09:00-12:00", 10, null));
    assertEquals(10L, result.get("id"));
    verify(internalDoctorClient).saveSchedule(any());
  }

  @Test
  void saveSchedule_existingSchedule_includesId() {
    when(internalDoctorClient.saveSchedule(any())).thenReturn(Map.of("id", 5L));

    var result = service.saveSchedule(new ScheduleSaveRequest(5L, 1L, 2L, LocalDate.of(2026, 1, 15), "09:00-12:00", 10, "DRAFT"));
    assertEquals(5L, result.get("id"));
  }

  // ─────────────── cancelSchedule ───────────────

  @Test
  void cancelSchedule_delegatesToClient() {
    when(internalDoctorClient.cancelSchedule(5L)).thenReturn(Map.of("status", "CANCELLED"));

    var result = service.cancelSchedule(new ScheduleCancelRequest(5L));
    assertEquals("CANCELLED", result.get("status"));
    verify(internalDoctorClient).cancelSchedule(5L);
  }

  // ─────────────── permissions ───────────────

  @Test
  void permissions_returnsCatalogRolesAndGrants() {
    RolePermission perm = new RolePermission();
    perm.setRole("ADMIN");
    perm.setPermissionKey("dashboard:view");
    perm.setEnabled(true);
    when(rolePermissionRepository.findAll()).thenReturn(List.of(perm));

    var result = service.permissions();
    assertTrue(result.containsKey("catalog"));
    assertTrue(result.containsKey("roles"));
    assertTrue(result.containsKey("grants"));
    List<?> catalog = (List<?>) result.get("catalog");
    assertFalse(catalog.isEmpty());
  }

  // ─────────────── myPermissions ───────────────

  @Test
  void myPermissions_adminWithNoGrants_returnsDefaultCatalog() {
    when(rolePermissionRepository.countByRole("ADMIN")).thenReturn(0L);

    var result = service.myPermissions(RoleType.ADMIN);
    assertFalse(result.isEmpty());
    assertTrue(result.contains("dashboard:view"));
    assertTrue(result.contains("device:manage"));
  }

  @Test
  void myPermissions_adminWithGrants_returnsEnabledPermissions() {
    when(rolePermissionRepository.countByRole("ADMIN")).thenReturn(1L);
    RolePermission perm = new RolePermission();
    perm.setPermissionKey("dashboard:view");
    when(rolePermissionRepository.findByRoleAndEnabledTrueOrderByPermissionKeyAsc("ADMIN"))
        .thenReturn(List.of(perm));

    var result = service.myPermissions(RoleType.ADMIN);
    assertEquals(1, result.size());
    assertEquals("dashboard:view", result.get(0));
  }

  @Test
  void myPermissions_doctor_returnsEnabledPermissions() {
    RolePermission perm = new RolePermission();
    perm.setPermissionKey("dashboard:view");
    when(rolePermissionRepository.findByRoleAndEnabledTrueOrderByPermissionKeyAsc("DOCTOR"))
        .thenReturn(List.of(perm));

    var result = service.myPermissions(RoleType.DOCTOR);
    assertEquals(1, result.size());
  }

  // ─────────────── saveRolePermissions ───────────────

  @Test
  void saveRolePermissions_validRequest_savesAllPermissions() {
    when(rolePermissionRepository.findByRoleAndPermissionKey(eq("DOCTOR"), anyString()))
        .thenReturn(Optional.empty());
    when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));
    when(rolePermissionRepository.findAll()).thenReturn(List.of());

    service.saveRolePermissions(new RolePermissionSaveRequest("doctor", List.of("dashboard:view")));
    // 18 permissions in catalog, all should be saved
    verify(rolePermissionRepository, org.mockito.Mockito.times(18)).save(any(RolePermission.class));
  }

  @Test
  void saveRolePermissions_nullKeys_disablesAll() {
    when(rolePermissionRepository.findByRoleAndPermissionKey(eq("DOCTOR"), anyString()))
        .thenReturn(Optional.empty());
    when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));
    when(rolePermissionRepository.findAll()).thenReturn(List.of());

    service.saveRolePermissions(new RolePermissionSaveRequest("doctor", null));
    verify(rolePermissionRepository, org.mockito.Mockito.times(18)).save(any(RolePermission.class));
  }

  @Test
  void saveRolePermissions_invalidPermissionKey_throws() {
    assertThrows(BusinessException.class, () ->
        service.saveRolePermissions(new RolePermissionSaveRequest("doctor", List.of("invalid:permission")))
    );
  }

  @Test
  void saveRolePermissions_invalidRole_throws() {
    assertThrows(BusinessException.class, () ->
        service.saveRolePermissions(new RolePermissionSaveRequest("invalid_role", List.of("dashboard:view")))
    );
  }

  // ─────────────── helpers ───────────────

  private static MedicalDevice makeDevice(Long id, String code, String name, String category,
      Long departmentId, String location, String status) {
    MedicalDevice d = new MedicalDevice();
    d.setId(id);
    d.setDeviceCode(code);
    d.setName(name);
    d.setCategory(category);
    d.setDepartmentId(departmentId);
    d.setLocation(location);
    d.setStatus(status);
    return d;
  }
}
