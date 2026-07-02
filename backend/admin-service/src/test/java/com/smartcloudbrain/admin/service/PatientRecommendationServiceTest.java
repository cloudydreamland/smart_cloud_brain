package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationSortRequest;
import com.smartcloudbrain.admin.dto.admin.PatientRecommendationStatusRequest;
import com.smartcloudbrain.admin.entity.Department;
import com.smartcloudbrain.admin.entity.Doctor;
import com.smartcloudbrain.admin.entity.PatientRecommendation;
import com.smartcloudbrain.admin.repository.DepartmentRepository;
import com.smartcloudbrain.admin.repository.DoctorRepository;
import com.smartcloudbrain.admin.repository.PatientRecommendationRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientRecommendationServiceTest {

  @Mock private PatientRecommendationRepository repository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private DoctorRepository doctorRepository;
  @InjectMocks private PatientRecommendationService service;

  // ── adminList ──────────────────────────────────────────────

  @Test
  void adminListReturnsAllNonDeletedForType() {
    PatientRecommendation r1 = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    PatientRecommendation r2 = recommendation(2L, "DEPARTMENT", 20L, "DISABLED", false);
    Department d1 = department(10L, "心内科", "胸痛");
    Department d2 = department(20L, "骨科", "骨折");

    when(repository.findByRecommendTypeAndDeletedFalseOrderBySortAscUpdatedAtDesc("DEPARTMENT"))
        .thenReturn(List.of(r1, r2));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(d1));
    when(departmentRepository.findById(20L)).thenReturn(Optional.of(d2));

    List<Map<String, Object>> rows = service.adminList("DEPARTMENT");

    assertEquals(2, rows.size());
    assertEquals("心内科", rows.get(0).get("targetName"));
    assertEquals("骨科", rows.get(1).get("targetName"));
  }

  @Test
  void adminListForDoctorType() {
    PatientRecommendation r1 = recommendation(1L, "DOCTOR", 11L, "ENABLED", false);
    Doctor doc = doctor(11L, "王医生", "ENABLED");
    Department dept = department(3L, "神经内科", "神经系统疾病");

    when(repository.findByRecommendTypeAndDeletedFalseOrderBySortAscUpdatedAtDesc("DOCTOR"))
        .thenReturn(List.of(r1));
    when(doctorRepository.findById(11L)).thenReturn(Optional.of(doc));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(dept));

    List<Map<String, Object>> rows = service.adminList("DOCTOR");

    assertEquals(1, rows.size());
    assertEquals("王医生", rows.get(0).get("targetName"));
    assertEquals("主任医师", rows.get(0).get("doctorTitle"));
    assertEquals("头痛眩晕", rows.get(0).get("specialty"));
  }

  @Test
  void adminListRejectsInvalidType() {
    assertThrows(BusinessException.class, () -> service.adminList("INVALID"));
  }

  @Test
  void adminListRejectsNullType() {
    assertThrows(BusinessException.class, () -> service.adminList(null));
  }

  // ── publicList ─────────────────────────────────────────────

  @Test
  void publicDepartmentListFiltersMissingTargets() {
    PatientRecommendation valid = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    PatientRecommendation missing = recommendation(2L, "DEPARTMENT", 99L, "ENABLED", false);
    Department department = department(10L, "心内科", "胸痛与慢病管理");

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DEPARTMENT", "ENABLED"))
        .thenReturn(List.of(valid, missing));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
    when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

    List<Map<String, Object>> rows = service.publicList("DEPARTMENT");

    assertEquals(1, rows.size());
    assertEquals("心内科", rows.get(0).get("targetName"));
    assertEquals("胸痛与慢病管理", rows.get(0).get("description"));
  }

  @Test
  void publicDoctorListFiltersDisabledDoctors() {
    PatientRecommendation enabled = recommendation(1L, "DOCTOR", 11L, "ENABLED", false);
    PatientRecommendation disabled = recommendation(2L, "DOCTOR", 12L, "ENABLED", false);
    Doctor doctor = doctor(11L, "王医生", "ENABLED");
    Doctor disabledDoctor = doctor(12L, "李医生", "DISABLED");

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DOCTOR", "ENABLED"))
        .thenReturn(List.of(enabled, disabled));
    when(doctorRepository.findById(11L)).thenReturn(Optional.of(doctor));
    when(doctorRepository.findById(12L)).thenReturn(Optional.of(disabledDoctor));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(department(3L, "神经内科", "神经系统疾病")));

    List<Map<String, Object>> rows = service.publicList("doctor");

    assertEquals(1, rows.size());
    assertEquals("王医生", rows.get(0).get("targetName"));
    assertEquals("神经内科", rows.get(0).get("departmentName"));
  }

  @Test
  void publicListFiltersDoctorWhenDoctorMissingFromRepo() {
    PatientRecommendation rec = recommendation(1L, "DOCTOR", 999L, "ENABLED", false);

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DOCTOR", "ENABLED"))
        .thenReturn(List.of(rec));
    when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

    List<Map<String, Object>> rows = service.publicList("DOCTOR");

    assertTrue(rows.isEmpty());
  }

  @Test
  void publicDoctorListWithNullDepartmentId() {
    PatientRecommendation rec = recommendation(1L, "DOCTOR", 11L, "ENABLED", false);
    Doctor doc = doctor(11L, "王医生", "ENABLED");
    doc.setDepartmentId(null);

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DOCTOR", "ENABLED"))
        .thenReturn(List.of(rec));
    when(doctorRepository.findById(11L)).thenReturn(Optional.of(doc));

    List<Map<String, Object>> rows = service.publicList("DOCTOR");

    assertEquals(1, rows.size());
    assertEquals("王医生", rows.get(0).get("targetName"));
    assertEquals("", rows.get(0).get("departmentName"));
  }

  @Test
  void publicDoctorListWithDepartmentNotFound() {
    PatientRecommendation rec = recommendation(1L, "DOCTOR", 11L, "ENABLED", false);
    Doctor doc = doctor(11L, "王医生", "ENABLED");
    doc.setDepartmentId(999L);

    when(repository.findByRecommendTypeAndStatusAndDeletedFalseOrderBySortAscUpdatedAtDesc("DOCTOR", "ENABLED"))
        .thenReturn(List.of(rec));
    when(doctorRepository.findById(11L)).thenReturn(Optional.of(doc));
    when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

    List<Map<String, Object>> rows = service.publicList("DOCTOR");

    assertEquals(1, rows.size());
    assertEquals("", rows.get(0).get("departmentName"));
  }

  // ── save: validation ───────────────────────────────────────

  @Test
  void saveRejectsUnknownTypeAndMissingTarget() {
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "BAD", 1L, "", "", "", "", 0, "ENABLED"), 7L));
    when(departmentRepository.findById(404L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "DEPARTMENT", 404L, "", "", "", "", 0, "ENABLED"), 7L));
  }

  @Test
  void saveRejectsNullTargetId() {
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "DEPARTMENT", null, "", "", "", "", 0, "ENABLED"), 7L));
  }

  @Test
  void saveRejectsInvalidStatus() {
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "DEPARTMENT", 10L, "", "", "", "", 0, "INVALID"), 7L));
  }

  @Test
  void saveRejectsNonexistentDoctorTarget() {
    when(doctorRepository.findById(404L)).thenReturn(Optional.empty());
    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        null, "DOCTOR", 404L, "", "", "", "", 0, "ENABLED"), 7L));
  }

  // ── save: create path ──────────────────────────────────────

  @Test
  void saveNormalizesTypeStatusAndAuditFields() {
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "胸痛")));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> {
      PatientRecommendation recommendation = invocation.getArgument(0);
      recommendation.setId(5L);
      return recommendation;
    });

    Map<String, Object> row = service.save(new PatientRecommendationSaveRequest(
        null, "department", 10L, " 首页展示 ", "", "", "", null, null), 8L);

    assertEquals(5L, row.get("id"));
    assertEquals("DEPARTMENT", row.get("recommendType"));
    assertEquals("首页展示", row.get("title"));
    assertEquals("ENABLED", row.get("status"));
    assertEquals(8L, row.get("createdBy"));
    assertEquals(8L, row.get("updatedBy"));
  }

  @Test
  void saveWithDoctorTypeAndAllFields() {
    Doctor doc = doctor(11L, "王医生", "ENABLED");
    Department dept = department(3L, "神经内科", "神经系统疾病");
    when(doctorRepository.findById(11L)).thenReturn(Optional.of(doc));
    when(departmentRepository.findById(3L)).thenReturn(Optional.of(dept));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> {
      PatientRecommendation rec = invocation.getArgument(0);
      rec.setId(6L);
      return rec;
    });

    Map<String, Object> row = service.save(new PatientRecommendationSaveRequest(
        null, "doctor", 11L, "推荐医生", "专治头痛", "https://img.com/doc.jpg", "key123", 5, "DISABLED"), 9L);

    assertEquals(6L, row.get("id"));
    assertEquals("DOCTOR", row.get("recommendType"));
    assertEquals("推荐医生", row.get("title"));
    assertEquals("专治头痛", row.get("description"));
    assertEquals("https://img.com/doc.jpg", row.get("imageUrl"));
    assertEquals("key123", row.get("imageObjectKey"));
    assertEquals("DISABLED", row.get("status"));
    assertEquals(5, row.get("sort"));
  }

  @Test
  void saveBlanksConvertedToNull() {
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> {
      PatientRecommendation rec = invocation.getArgument(0);
      rec.setId(7L);
      return rec;
    });

    Map<String, Object> row = service.save(new PatientRecommendationSaveRequest(
        null, "DEPARTMENT", 10L, "  ", "  ", "  ", "  ", 0, "ENABLED"), 1L);

    // blank title/description fall back to target name/description in view()
    assertEquals("心内科", row.get("title"));
    assertEquals("", row.get("description"));  // dept description is ""
    assertEquals("", row.get("imageUrl"));
    assertEquals("", row.get("imageObjectKey"));
  }

  // ── save: update path ──────────────────────────────────────

  @Test
  void saveUpdatesExistingRecommendation() {
    PatientRecommendation existing = recommendation(5L, "DEPARTMENT", 10L, "ENABLED", false);
    when(repository.findById(5L)).thenReturn(Optional.of(existing));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> row = service.save(new PatientRecommendationSaveRequest(
        5L, "DEPARTMENT", 10L, "新标题", "新描述", "", "", 3, "DISABLED"), 2L);

    assertEquals(5L, row.get("id"));
    assertEquals("新标题", row.get("title"));
    assertEquals("新描述", row.get("description"));
    assertEquals("DISABLED", row.get("status"));
    assertEquals(3, row.get("sort"));
    assertEquals(2L, row.get("updatedBy"));
  }

  @Test
  void saveThrowsWhenUpdatingDeletedRecommendation() {
    PatientRecommendation deleted = recommendation(5L, "DEPARTMENT", 10L, "ENABLED", true);
    when(repository.findById(5L)).thenReturn(Optional.of(deleted));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));

    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        5L, "DEPARTMENT", 10L, "标题", "", "", "", 0, "ENABLED"), 1L));
  }

  @Test
  void saveThrowsWhenUpdatingNonexistentRecommendation() {
    when(repository.findById(999L)).thenReturn(Optional.empty());
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));

    assertThrows(BusinessException.class, () -> service.save(new PatientRecommendationSaveRequest(
        999L, "DEPARTMENT", 10L, "标题", "", "", "", 0, "ENABLED"), 1L));
  }

  // ── updateStatus ───────────────────────────────────────────

  @Test
  void updateStatusChangesStatusAndAuditFields() {
    PatientRecommendation rec = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(rec));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));

    Map<String, Object> row = service.updateStatus(
        new PatientRecommendationStatusRequest(1L, "DISABLED"), 3L);

    assertEquals("DISABLED", row.get("status"));
    assertEquals(3L, row.get("updatedBy"));
  }

  @Test
  void updateStatusDefaultsToEnabledWhenNull() {
    PatientRecommendation rec = recommendation(1L, "DEPARTMENT", 10L, "DISABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(rec));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));

    Map<String, Object> row = service.updateStatus(
        new PatientRecommendationStatusRequest(1L, null), 3L);

    assertEquals("ENABLED", row.get("status"));
  }

  @Test
  void updateStatusRejectsInvalidStatus() {
    PatientRecommendation rec = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(rec));

    assertThrows(BusinessException.class, () ->
        service.updateStatus(new PatientRecommendationStatusRequest(1L, "INVALID"), 3L));
  }

  @Test
  void updateStatusRejectsDeletedRecommendation() {
    PatientRecommendation deleted = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", true);
    when(repository.findById(1L)).thenReturn(Optional.of(deleted));

    assertThrows(BusinessException.class, () ->
        service.updateStatus(new PatientRecommendationStatusRequest(1L, "DISABLED"), 3L));
  }

  @Test
  void updateStatusRejectsMissingRecommendation() {
    when(repository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () ->
        service.updateStatus(new PatientRecommendationStatusRequest(999L, "DISABLED"), 3L));
  }

  // ── delete ─────────────────────────────────────────────────

  @Test
  void deleteMarksRecommendationDeleted() {
    PatientRecommendation recommendation = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(recommendation));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(department(10L, "心内科", "")));

    service.delete(1L, 6L);

    ArgumentCaptor<PatientRecommendation> captor = ArgumentCaptor.forClass(PatientRecommendation.class);
    verify(repository).save(captor.capture());
    assertEquals(true, captor.getValue().getDeleted());
    assertEquals(6L, captor.getValue().getUpdatedBy());
  }

  @Test
  void deleteRejectsDeletedOrMissingRecommendation() {
    PatientRecommendation deleted = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", true);
    when(repository.findById(1L)).thenReturn(Optional.of(deleted));
    when(repository.findById(2L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.delete(1L, 6L));
    assertThrows(BusinessException.class, () -> service.delete(2L, 6L));
  }

  // ── sort ───────────────────────────────────────────────────

  @Test
  void sortRejectsEmptyListAndCrossTypeRows() {
    assertThrows(BusinessException.class, () -> service.sort(new PatientRecommendationSortRequest(List.of()), 1L));

    PatientRecommendation department = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    PatientRecommendation doctor = recommendation(2L, "DOCTOR", 11L, "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(department));
    when(repository.findById(2L)).thenReturn(Optional.of(doctor));

    assertThrows(BusinessException.class, () -> service.sort(new PatientRecommendationSortRequest(List.of(
        new PatientRecommendationSortRequest.SortItem(1L, 10),
        new PatientRecommendationSortRequest.SortItem(2L, 20)
    )), 9L));
  }

  @Test
  void sortUpdatesSortFieldAndReturnsAdminList() {
    PatientRecommendation r1 = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", false);
    PatientRecommendation r2 = recommendation(2L, "DEPARTMENT", 20L, "ENABLED", false);
    Department d1 = department(10L, "心内科", "");
    Department d2 = department(20L, "骨科", "");

    when(repository.findById(1L)).thenReturn(Optional.of(r1));
    when(repository.findById(2L)).thenReturn(Optional.of(r2));
    when(repository.save(any(PatientRecommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.findByRecommendTypeAndDeletedFalseOrderBySortAscUpdatedAtDesc("DEPARTMENT"))
        .thenReturn(List.of(r1, r2));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(d1));
    when(departmentRepository.findById(20L)).thenReturn(Optional.of(d2));

    PatientRecommendationSortRequest sortRequest = new PatientRecommendationSortRequest(List.of(
        new PatientRecommendationSortRequest.SortItem(1L, 10),
        new PatientRecommendationSortRequest.SortItem(2L, 20)
    ));

    List<Map<String, Object>> rows = service.sort(sortRequest, 5L);

    assertEquals(2, rows.size());
    assertEquals(10, r1.getSort());
    assertEquals(20, r2.getSort());
    assertEquals(5L, r1.getUpdatedBy());
    assertEquals(5L, r2.getUpdatedBy());
  }

  @Test
  void sortRejectsDeletedItem() {
    PatientRecommendation deleted = recommendation(1L, "DEPARTMENT", 10L, "ENABLED", true);
    when(repository.findById(1L)).thenReturn(Optional.of(deleted));

    assertThrows(BusinessException.class, () -> service.sort(new PatientRecommendationSortRequest(List.of(
        new PatientRecommendationSortRequest.SortItem(1L, 10)
    )), 5L));
  }

  // ── view: null/edge field handling ─────────────────────────

  @Test
  void viewHandlesNullDatesAndFields() {
    PatientRecommendation rec = new PatientRecommendation();
    rec.setId(1L);
    rec.setRecommendType("DEPARTMENT");
    rec.setTargetId(10L);
    rec.setTitle(null);
    rec.setDescription(null);
    rec.setImageUrl(null);
    rec.setImageObjectKey(null);
    rec.setSort(null);
    rec.setStatus(null);
    rec.setDeleted(false);
    rec.setCreatedBy(null);
    rec.setUpdatedBy(null);
    rec.setCreatedAt(null);
    rec.setUpdatedAt(null);

    Department dept = department(10L, "心内科", "心血管疾病");
    when(repository.findByRecommendTypeAndDeletedFalseOrderBySortAscUpdatedAtDesc("DEPARTMENT"))
        .thenReturn(List.of(rec));
    when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));

    List<Map<String, Object>> rows = service.adminList("DEPARTMENT");

    Map<String, Object> row = rows.get(0);
    assertEquals("心内科", row.get("title"));       // falls back to target name
    assertEquals("心血管疾病", row.get("description")); // falls back to target description
    assertEquals("", row.get("imageUrl"));
    assertEquals("", row.get("imageObjectKey"));
    assertEquals(0, row.get("sort"));
    assertEquals("ENABLED", row.get("status"));  // default
    assertEquals("", row.get("createdAt"));
    assertEquals("", row.get("updatedAt"));
  }

  // ── helpers ────────────────────────────────────────────────

  private PatientRecommendation recommendation(Long id, String type, Long targetId, String status, boolean deleted) {
    PatientRecommendation recommendation = new PatientRecommendation();
    recommendation.setId(id);
    recommendation.setRecommendType(type);
    recommendation.setTargetId(targetId);
    recommendation.setStatus(status);
    recommendation.setDeleted(deleted);
    recommendation.setSort(0);
    return recommendation;
  }

  private Department department(Long id, String name, String description) {
    Department department = new Department();
    department.setId(id);
    department.setName(name);
    department.setDescription(description);
    return department;
  }

  private Doctor doctor(Long id, String name, String status) {
    Doctor doctor = new Doctor();
    doctor.setId(id);
    doctor.setName(name);
    doctor.setDepartmentId(3L);
    doctor.setTitle("主任医师");
    doctor.setSpecialty("头痛眩晕");
    doctor.setStatus(status);
    return doctor;
  }
}
