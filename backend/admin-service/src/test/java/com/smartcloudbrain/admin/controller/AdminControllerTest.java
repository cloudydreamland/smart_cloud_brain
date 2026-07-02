package com.smartcloudbrain.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.dto.admin.AccountSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DepartmentSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceStatusRequest;
import com.smartcloudbrain.admin.dto.admin.DeviceUsageSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DoctorSaveRequest;
import com.smartcloudbrain.admin.dto.admin.DrugSaveRequest;
import com.smartcloudbrain.admin.dto.admin.EmailConfigSaveRequest;
import com.smartcloudbrain.admin.dto.admin.EmailConfigTestRequest;
import com.smartcloudbrain.admin.dto.admin.PatientSaveRequest;
import com.smartcloudbrain.admin.dto.admin.RolePermissionSaveRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleCancelRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleGenerateRequest;
import com.smartcloudbrain.admin.dto.admin.SchedulePublishRequest;
import com.smartcloudbrain.admin.dto.admin.ScheduleSaveRequest;
import com.smartcloudbrain.admin.dto.admin.TriageAssignRequest;
import com.smartcloudbrain.admin.service.AdminCatalogService;
import com.smartcloudbrain.admin.service.AdminOperationsService;
import com.smartcloudbrain.admin.service.SystemEmailConfigService;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.security.AuthenticatedUser;
import com.smartcloudbrain.common.security.CurrentUserService;
import com.smartcloudbrain.common.security.RoleType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock private AdminCatalogService adminCatalogService;
  @Mock private AdminOperationsService adminOperationsService;
  @Mock private SystemEmailConfigService systemEmailConfigService;
  @Mock private CurrentUserService currentUserService;

  private AdminController controller;
  private final AuthenticatedUser adminUser = new AuthenticatedUser(1L, RoleType.ADMIN, "Admin");

  @BeforeEach
  void setUp() {
    controller = new AdminController(
        adminCatalogService,
        adminOperationsService,
        systemEmailConfigService,
        currentUserService
    );
  }

  // ─── helper ────────────────────────────────────────────────────────────

  private void stubAdmin() {
    when(currentUserService.require(RoleType.ADMIN)).thenReturn(adminUser);
  }

  private void stubAdminPermission(String key) {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, key)).thenReturn(true);
  }

  // ─── requireAdmin() happy paths ────────────────────────────────────────

  @Test
  void departments_returnsSuccess() {
    stubAdmin();
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "name", "内科"));
    when(adminCatalogService.departments()).thenReturn(expected);

    Result<?> result = controller.departments();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(adminCatalogService).departments();
  }

  @Test
  void doctors_returnsSuccess() {
    stubAdmin();
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "name", "张医生"));
    when(adminCatalogService.doctors(2L)).thenReturn(expected);

    Result<?> result = controller.doctors(2L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(adminCatalogService).doctors(2L);
  }

  @Test
  void doctors_nullDepartmentId_returnsSuccess() {
    stubAdmin();
    when(adminCatalogService.doctors(null)).thenReturn(List.of());

    Result<?> result = controller.doctors(null);

    assertEquals(0, result.code());
    verify(adminCatalogService).doctors(null);
  }

  @Test
  void accounts_returnsSuccess() {
    stubAdmin();
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "account", "admin"));
    when(adminCatalogService.accounts()).thenReturn(expected);

    Result<?> result = controller.accounts();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void roles_returnsSuccess() {
    stubAdmin();
    List<Map<String, Object>> expected = List.of(Map.of("role", "ADMIN", "label", "系统管理员"));
    when(adminCatalogService.roles()).thenReturn(expected);

    Result<?> result = controller.roles();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void saveAccount_returnsSuccess() {
    stubAdmin();
    AccountSaveRequest request = new AccountSaveRequest(null, "ADMIN", "admin2", "管理员二", "123456", null, null, null, null);
    Map<String, Object> expected = Map.of("id", 2, "account", "admin2");
    when(adminCatalogService.saveAccount(any(AccountSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveAccount(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(adminCatalogService).saveAccount(request);
  }

  @Test
  void saveDepartment_returnsSuccess() {
    stubAdmin();
    DepartmentSaveRequest request = new DepartmentSaveRequest(null, "ICU", "重症医学科", "ICU描述");
    Map<String, Object> expected = Map.of("id", 1, "name", "重症医学科");
    when(adminCatalogService.saveDepartment(any(DepartmentSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveDepartment(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void saveDoctor_returnsSuccess() {
    stubAdmin();
    DoctorSaveRequest request = new DoctorSaveRequest(null, "李医生", "13900000002", "123456", 1L, "主任医师", "内科", null);
    Map<String, Object> expected = Map.of("id", 1, "name", "李医生");
    when(adminCatalogService.saveDoctor(any(DoctorSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveDoctor(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void drugs_returnsSuccess() {
    stubAdmin();
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "name", "阿莫西林"));
    when(adminCatalogService.drugs()).thenReturn(expected);

    Result<?> result = controller.drugs();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void saveDrug_returnsSuccess() {
    stubAdmin();
    DrugSaveRequest request = new DrugSaveRequest(null, "布洛芬", "200mg", "过敏者禁用", null, null);
    Map<String, Object> expected = Map.of("id", 1, "name", "布洛芬");
    when(adminCatalogService.saveDrug(any(DrugSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveDrug(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void aiLogs_returnsSuccess() {
    stubAdmin();
    Object logs = List.of(Map.of("id", 1, "prompt", "test"));
    when(adminCatalogService.aiLogs()).thenReturn(logs);

    Result<?> result = controller.aiLogs();

    assertEquals(0, result.code());
    assertEquals(logs, result.data());
  }

  @Test
  void generateSchedule_returnsSuccess() {
    stubAdmin();
    ScheduleGenerateRequest request = new ScheduleGenerateRequest(LocalDate.now(), 3);
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "status", "DRAFT"));
    when(adminCatalogService.generateScheduleSuggestions(any(ScheduleGenerateRequest.class))).thenReturn(expected);

    Result<?> result = controller.generateSchedule(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void publishSchedule_returnsSuccess() {
    stubAdmin();
    SchedulePublishRequest request = new SchedulePublishRequest(List.of(1L, 2L));
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "status", "PUBLISHED"));
    when(adminCatalogService.publishSchedule(any(SchedulePublishRequest.class))).thenReturn(expected);

    Result<?> result = controller.publishSchedule(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void scheduleSuggestionDetail_returnsSuccess() {
    stubAdmin();
    Map<String, Object> expected = Map.of("id", 1, "status", "DRAFT");
    when(adminCatalogService.scheduleSuggestionDetail(1L)).thenReturn(expected);

    Result<?> result = controller.scheduleSuggestionDetail(1L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void triageDesk_returnsSuccess() {
    stubAdmin();
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "status", "WAITING"));
    when(adminCatalogService.triageDesk()).thenReturn(expected);

    Result<?> result = controller.triageDesk();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void triageDetail_returnsSuccess() {
    stubAdmin();
    Map<String, Object> expected = Map.of("id", 1, "status", "WAITING");
    when(adminCatalogService.triageDetail(1L)).thenReturn(expected);

    Result<?> result = controller.triageDetail(1L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void assignTriage_returnsSuccess() {
    stubAdmin();
    TriageAssignRequest request = new TriageAssignRequest(1L, 2L);
    Map<String, Object> expected = Map.of("id", 1, "status", "ASSIGNED");
    when(adminCatalogService.assignTriage(1L, 2L)).thenReturn(expected);

    Result<?> result = controller.assignTriage(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(adminCatalogService).assignTriage(1L, 2L);
  }

  @Test
  void closeTriage_returnsSuccess() {
    stubAdmin();
    Map<String, Object> expected = Map.of("id", 1, "status", "CLOSED");
    when(adminCatalogService.closeTriage(1L)).thenReturn(expected);

    Result<?> result = controller.closeTriage(Map.of("triageRecordId", 1L));

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(adminCatalogService).closeTriage(1L);
  }

  // ─── requireAdminPermission() happy paths ──────────────────────────────

  @Test
  void schedules_withPermission_returnsSuccess() {
    stubAdminPermission("schedule:manage");
    List<Map<String, Object>> expected = List.of(Map.of("id", 1));
    when(adminOperationsService.schedules(null, null, null, null, null)).thenReturn(expected);

    Result<?> result = controller.schedules(null, null, null, null, null);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void schedules_withFilters_returnsSuccess() {
    stubAdminPermission("schedule:manage");
    when(adminOperationsService.schedules("2026-01-01", "2026-01-31", 1L, 2L, "PUBLISHED"))
        .thenReturn(List.of());

    Result<?> result = controller.schedules("2026-01-01", "2026-01-31", 1L, 2L, "PUBLISHED");

    assertEquals(0, result.code());
  }

  @Test
  void saveSchedule_returnsSuccess() {
    stubAdminPermission("schedule:manage");
    ScheduleSaveRequest request = new ScheduleSaveRequest(null, 1L, 1L, LocalDate.now(), "09:00-12:00", 10, null);
    Map<String, Object> expected = Map.of("id", 1, "status", "PUBLISHED");
    when(adminOperationsService.saveSchedule(any(ScheduleSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveSchedule(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void cancelSchedule_returnsSuccess() {
    stubAdminPermission("schedule:manage");
    ScheduleCancelRequest request = new ScheduleCancelRequest(1L);
    Map<String, Object> expected = Map.of("id", 1, "status", "CANCELLED");
    when(adminOperationsService.cancelSchedule(any(ScheduleCancelRequest.class))).thenReturn(expected);

    Result<?> result = controller.cancelSchedule(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void devices_returnsSuccess() {
    stubAdminPermission("device:manage");
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "name", "CT"));
    when(adminOperationsService.devices(null, null, null, null)).thenReturn(expected);

    Result<?> result = controller.devices(null, null, null, null);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void devices_withFilters_returnsSuccess() {
    stubAdminPermission("device:manage");
    when(adminOperationsService.devices("CT", 1L, "影像", "AVAILABLE")).thenReturn(List.of());

    Result<?> result = controller.devices("CT", 1L, "影像", "AVAILABLE");

    assertEquals(0, result.code());
  }

  @Test
  void saveDevice_returnsSuccess() {
    stubAdminPermission("device:manage");
    DeviceSaveRequest request = new DeviceSaveRequest(null, "CT-001", "CT扫描仪", "影像", 1L, "1楼", null, null, null);
    Map<String, Object> expected = Map.of("id", 1, "deviceCode", "CT-001");
    when(adminOperationsService.saveDevice(any(DeviceSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveDevice(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void updateDeviceStatus_returnsSuccess() {
    stubAdminPermission("device:manage");
    DeviceStatusRequest request = new DeviceStatusRequest(1L, "MAINTENANCE");
    Map<String, Object> expected = Map.of("id", 1, "status", "MAINTENANCE");
    when(adminOperationsService.updateDeviceStatus(any(DeviceStatusRequest.class))).thenReturn(expected);

    Result<?> result = controller.updateDeviceStatus(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void deviceUsageList_returnsSuccess() {
    stubAdminPermission("device:manage");
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "usageType", "CLINICAL"));
    when(adminOperationsService.deviceUsages(1L)).thenReturn(expected);

    Result<?> result = controller.deviceUsageList(1L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void deviceUsageList_nullDeviceId_returnsSuccess() {
    stubAdminPermission("device:manage");
    when(adminOperationsService.deviceUsages(null)).thenReturn(List.of());

    Result<?> result = controller.deviceUsageList(null);

    assertEquals(0, result.code());
  }

  @Test
  void saveDeviceUsage_returnsSuccess() {
    stubAdminPermission("device:manage");
    DeviceUsageSaveRequest request = new DeviceUsageSaveRequest(null, 1L, "CLINICAL", "张医生", null, null, null, null, null);
    Map<String, Object> expected = Map.of("id", 1, "usageType", "CLINICAL");
    when(adminOperationsService.saveDeviceUsage(any(DeviceUsageSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveDeviceUsage(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void patients_returnsSuccess() {
    stubAdminPermission("patient:manage");
    List<Map<String, Object>> expected = List.of(Map.of("id", 1, "name", "张三"));
    when(adminOperationsService.patients(null, null, null, null)).thenReturn(expected);

    Result<?> result = controller.patients(null, null, null, null);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void patients_withFilters_returnsSuccess() {
    stubAdminPermission("patient:manage");
    when(adminOperationsService.patients("张", "男", 18, 60)).thenReturn(List.of());

    Result<?> result = controller.patients("张", "男", 18, 60);

    assertEquals(0, result.code());
  }

  @Test
  void patientDetail_returnsSuccess() {
    stubAdminPermission("patient:manage");
    Map<String, Object> expected = Map.of("id", 1, "name", "张三");
    when(adminOperationsService.patientDetail(1L)).thenReturn(expected);

    Result<?> result = controller.patientDetail(1L);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void savePatient_returnsSuccess() {
    stubAdminPermission("patient:manage");
    PatientSaveRequest request = new PatientSaveRequest(1L, "张三", "男", 30, null, null);
    Map<String, Object> expected = Map.of("id", 1, "name", "张三");
    when(adminOperationsService.savePatient(any(PatientSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.savePatient(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void statisticsOverview_returnsSuccess() {
    stubAdminPermission("statistics:view");
    Map<String, Object> expected = Map.of("registrations", 100, "patients", 50);
    when(adminOperationsService.overview()).thenReturn(expected);

    Result<?> result = controller.statisticsOverview();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void dashboardStats_returnsSuccess() {
    stubAdminPermission("dashboard:view");
    Map<String, Object> expected = Map.of("registrations", 100);
    when(adminOperationsService.overview()).thenReturn(expected);

    Result<?> result = controller.dashboardStats();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void statisticsTrend_returnsSuccess() {
    stubAdminPermission("statistics:view");
    List<Map<String, Object>> expected = List.of(Map.of("day", "2026-01-01", "registrations", 10));
    when(adminOperationsService.trend("2026-01-01", "2026-01-31")).thenReturn(expected);

    Result<?> result = controller.statisticsTrend("2026-01-01", "2026-01-31");

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void doctorWorkload_returnsSuccess() {
    stubAdminPermission("statistics:view");
    List<Map<String, Object>> expected = List.of(Map.of("doctor_id", 1, "registrations", 20));
    when(adminOperationsService.doctorWorkload(null, null)).thenReturn(expected);

    Result<?> result = controller.doctorWorkload(null, null);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void patientDistribution_returnsSuccess() {
    stubAdminPermission("statistics:view");
    Map<String, Object> expected = Map.of("gender", List.of(), "age", List.of());
    when(adminOperationsService.patientDistribution()).thenReturn(expected);

    Result<?> result = controller.patientDistribution();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void deviceUsageStatistics_returnsSuccess() {
    stubAdminPermission("statistics:view");
    List<Map<String, Object>> expected = List.of(Map.of("device_id", 1, "usage_count", 5));
    when(adminOperationsService.deviceUsage()).thenReturn(expected);

    Result<?> result = controller.deviceUsageStatistics();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void report_returnsSuccess() {
    stubAdminPermission("statistics:export");
    List<Map<String, Object>> expected = List.of(Map.of("metric", "registrations", "value", 100));
    when(adminOperationsService.report("2026-01-01", "2026-01-31")).thenReturn(expected);

    Result<?> result = controller.report("2026-01-01", "2026-01-31");

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void permissions_returnsSuccess() {
    stubAdminPermission("permission:manage");
    Map<String, Object> expected = Map.of("catalog", List.of(), "roles", List.of(), "grants", List.of());
    when(adminOperationsService.permissions()).thenReturn(expected);

    Result<?> result = controller.permissions();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void myPermissions_returnsSuccess() {
    stubAdmin();
    List<String> expected = List.of("dashboard:view", "schedule:manage");
    when(adminOperationsService.myPermissions(RoleType.ADMIN)).thenReturn(expected);

    Result<?> result = controller.myPermissions();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(adminOperationsService).myPermissions(RoleType.ADMIN);
  }

  @Test
  void saveRolePermissions_returnsSuccess() {
    stubAdminPermission("permission:manage");
    RolePermissionSaveRequest request = new RolePermissionSaveRequest("DOCTOR", List.of("dashboard:view"));
    Map<String, Object> expected = Map.of("catalog", List.of(), "roles", List.of(), "grants", List.of());
    when(adminOperationsService.saveRolePermissions(any(RolePermissionSaveRequest.class))).thenReturn(expected);

    Result<?> result = controller.saveRolePermissions(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void emailConfig_returnsSuccess() {
    stubAdminPermission("system-config:manage");
    Map<String, Object> expected = Map.of("host", "smtp.example.com", "port", 465);
    when(systemEmailConfigService.getConfig()).thenReturn(expected);

    Result<?> result = controller.emailConfig();

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  @Test
  void saveEmailConfig_returnsSuccess() {
    stubAdminPermission("system-config:manage");
    EmailConfigSaveRequest request = new EmailConfigSaveRequest("smtp.example.com", 465, "user", "pass", "from@example.com", "系统", true, false, true);
    Map<String, Object> expected = Map.of("host", "smtp.example.com", "saved", true);
    when(systemEmailConfigService.save(any(EmailConfigSaveRequest.class), any(AuthenticatedUser.class))).thenReturn(expected);

    Result<?> result = controller.saveEmailConfig(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
    verify(systemEmailConfigService).save(eq(request), eq(adminUser));
  }

  @Test
  void testEmailConfig_returnsSuccess() {
    stubAdminPermission("system-config:manage");
    EmailConfigTestRequest request = new EmailConfigTestRequest("test@example.com");
    Map<String, Object> expected = Map.of("sent", true);
    when(systemEmailConfigService.sendTest(any(EmailConfigTestRequest.class))).thenReturn(expected);

    Result<?> result = controller.testEmailConfig(request);

    assertEquals(0, result.code());
    assertEquals(expected, result.data());
  }

  // ─── permission denied for requireAdminPermission methods ──────────────

  @Test
  void schedules_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "schedule:manage")).thenReturn(false);

    assertThrows(BusinessException.class,
        () -> controller.schedules(null, null, null, null, null));
  }

  @Test
  void devices_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "device:manage")).thenReturn(false);

    assertThrows(BusinessException.class,
        () -> controller.devices(null, null, null, null));
  }

  @Test
  void patients_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "patient:manage")).thenReturn(false);

    assertThrows(BusinessException.class,
        () -> controller.patients(null, null, null, null));
  }

  @Test
  void statisticsOverview_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "statistics:view")).thenReturn(false);

    assertThrows(BusinessException.class, controller::statisticsOverview);
  }

  @Test
  void permissions_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "permission:manage")).thenReturn(false);

    assertThrows(BusinessException.class, controller::permissions);
  }

  @Test
  void emailConfig_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "system-config:manage")).thenReturn(false);

    assertThrows(BusinessException.class, controller::emailConfig);
  }

  @Test
  void report_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "statistics:export")).thenReturn(false);

    assertThrows(BusinessException.class, () -> controller.report(null, null));
  }

  @Test
  void dashboardStats_permissionDenied_throws() {
    stubAdmin();
    when(adminOperationsService.hasPermission(RoleType.ADMIN, "dashboard:view")).thenReturn(false);

    assertThrows(BusinessException.class, controller::dashboardStats);
  }
}
