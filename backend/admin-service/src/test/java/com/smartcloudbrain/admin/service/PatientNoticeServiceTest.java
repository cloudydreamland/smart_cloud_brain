package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.dto.admin.PatientNoticeSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeSortRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeStatusRequest;
import com.smartcloudbrain.admin.entity.PatientNotice;
import com.smartcloudbrain.admin.repository.PatientNoticeRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDateTime;
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
class PatientNoticeServiceTest {

  @Mock private PatientNoticeRepository repository;
  @InjectMocks private PatientNoticeService service;

  // ── adminList ──────────────────────────────────────────────

  @Test
  void adminListReturnsViewForAllNonDeletedNotices() {
    PatientNotice n1 = notice(1L, "公告一", "内容一", "ENABLED", false);
    PatientNotice n2 = notice(2L, "公告二", "内容二", "DISABLED", false);
    n1.setPinned(true);
    n1.setSort(1);
    n2.setPinned(false);
    n2.setSort(2);
    when(repository.findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc())
        .thenReturn(List.of(n1, n2));

    List<Map<String, Object>> rows = service.adminList();

    assertEquals(2, rows.size());
    assertEquals("公告一", rows.get(0).get("title"));
    assertEquals("公告二", rows.get(1).get("title"));
    verify(repository).findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc();
  }

  @Test
  void adminListReturnsEmptyWhenNoNotices() {
    when(repository.findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc())
        .thenReturn(List.of());

    List<Map<String, Object>> rows = service.adminList();

    assertTrue(rows.isEmpty());
  }

  // ── publicList ─────────────────────────────────────────────

  @Test
  void publicListDelegatesEffectiveWindowFilteringToRepositoryAndBuildsView() {
    PatientNotice pinned = notice(1L, "置顶公告", "内容", "ENABLED", false);
    pinned.setPinned(true);
    pinned.setSort(5);
    when(repository.findPublicNotices(any(LocalDateTime.class))).thenReturn(List.of(pinned));

    List<Map<String, Object>> rows = service.publicList();

    assertEquals(1, rows.size());
    assertEquals("置顶公告", rows.get(0).get("title"));
    assertEquals(true, rows.get(0).get("pinned"));
    assertEquals(5, rows.get(0).get("sort"));
    verify(repository).findPublicNotices(any(LocalDateTime.class));
  }

  // ── save: validation ───────────────────────────────────────

  @Test
  void saveRejectsInvalidTimeWindowAndBlankContent() {
    LocalDateTime start = LocalDateTime.of(2026, 6, 29, 9, 0);
    LocalDateTime end = start.minusHours(1);

    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        null, "公告", "内容", "NONE", "", start, end, false, 0, "ENABLED"), 7L));
    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        null, "公告", " ", "NONE", "", null, null, false, 0, "ENABLED"), 7L));
  }

  @Test
  void saveRejectsNullTitle() {
    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        null, null, "内容", "NONE", "", null, null, false, 0, "ENABLED"), 7L));
  }

  @Test
  void saveRejectsInvalidStatus() {
    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        null, "公告", "内容", "NONE", "", null, null, false, 0, "INVALID"), 7L));
  }

  @Test
  void saveRejectsInvalidLinkType() {
    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        null, "公告", "内容", "BADTYPE", "", null, null, false, 0, "ENABLED"), 7L));
  }

  // ── save: new notice (create path) ─────────────────────────

  @Test
  void saveNormalizesDefaultsAndSetsAuditFields() {
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> {
      PatientNotice notice = invocation.getArgument(0);
      notice.setId(9L);
      return notice;
    });

    Map<String, Object> row = service.save(new PatientNoticeSaveRequest(
        null, " 新公告 ", " 内容 ", "", "  ", null, null, null, null, null), 3L);

    assertEquals(9L, row.get("id"));
    assertEquals("新公告", row.get("title"));
    assertEquals("内容", row.get("content"));
    assertEquals("NONE", row.get("linkType"));
    assertEquals("ENABLED", row.get("status"));
    assertEquals(3L, row.get("createdBy"));
    assertEquals(3L, row.get("updatedBy"));
  }

  @Test
  void saveWithValidTimeWindowAndAllFields() {
    LocalDateTime start = LocalDateTime.of(2026, 7, 1, 8, 0);
    LocalDateTime end = LocalDateTime.of(2026, 7, 31, 18, 0);
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> {
      PatientNotice notice = invocation.getArgument(0);
      notice.setId(10L);
      return notice;
    });

    Map<String, Object> row = service.save(new PatientNoticeSaveRequest(
        null, "夏令公告", "内容", "EXTERNAL", "https://example.com",
        start, end, true, 5, "ENABLED"), 4L);

    assertEquals(10L, row.get("id"));
    assertEquals("夏令公告", row.get("title"));
    assertEquals("EXTERNAL", row.get("linkType"));
    assertEquals("https://example.com", row.get("linkUrl"));
    assertEquals(true, row.get("pinned"));
    assertEquals(5, row.get("sort"));
    assertEquals(start.toString(), row.get("startTime"));
    assertEquals(end.toString(), row.get("endTime"));
  }

  @Test
  void saveWithInternalLinkType() {
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> {
      PatientNotice notice = invocation.getArgument(0);
      notice.setId(11L);
      return notice;
    });

    Map<String, Object> row = service.save(new PatientNoticeSaveRequest(
        null, "内部链接", "内容", "internal", "/page/detail",
        null, null, false, 0, "ENABLED"), 5L);

    assertEquals("INTERNAL", row.get("linkType"));
    assertEquals("/page/detail", row.get("linkUrl"));
  }

  @Test
  void saveTrimsTitleAndContent() {
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> {
      PatientNotice notice = invocation.getArgument(0);
      notice.setId(12L);
      return notice;
    });

    Map<String, Object> row = service.save(new PatientNoticeSaveRequest(
        null, "  带空格标题  ", "  带空格内容  ", null, null,
        null, null, false, 0, null), 1L);

    assertEquals("带空格标题", row.get("title"));
    assertEquals("带空格内容", row.get("content"));
  }

  // ── save: update path (existing id) ────────────────────────

  @Test
  void saveUpdatesExistingNotice() {
    PatientNotice existing = notice(5L, "旧标题", "旧内容", "ENABLED", false);
    when(repository.findById(5L)).thenReturn(Optional.of(existing));
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> row = service.save(new PatientNoticeSaveRequest(
        5L, "新标题", "新内容", "NONE", "", null, null, false, 0, "DISABLED"), 2L);

    assertEquals(5L, row.get("id"));
    assertEquals("新标题", row.get("title"));
    assertEquals("新内容", row.get("content"));
    assertEquals("DISABLED", row.get("status"));
    assertEquals(2L, row.get("updatedBy"));
    // createdBy should NOT be set on update (existing id != null)
    verify(repository).save(any(PatientNotice.class));
  }

  @Test
  void saveThrowsWhenUpdatingDeletedNotice() {
    PatientNotice deleted = notice(6L, "已删", "内容", "ENABLED", true);
    when(repository.findById(6L)).thenReturn(Optional.of(deleted));

    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        6L, "标题", "内容", "NONE", "", null, null, false, 0, "ENABLED"), 1L));
  }

  @Test
  void saveThrowsWhenUpdatingNonexistentNotice() {
    when(repository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.save(new PatientNoticeSaveRequest(
        999L, "标题", "内容", "NONE", "", null, null, false, 0, "ENABLED"), 1L));
  }

  // ── updateStatus ───────────────────────────────────────────

  @Test
  void updateStatusChangesStatusAndSetsAuditFields() {
    PatientNotice notice = notice(1L, "公告", "内容", "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(notice));
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> row = service.updateStatus(new PatientNoticeStatusRequest(1L, "DISABLED"), 3L);

    assertEquals("DISABLED", row.get("status"));
    assertEquals(3L, row.get("updatedBy"));
  }

  @Test
  void updateStatusRejectsInvalidStatus() {
    PatientNotice notice = notice(1L, "公告", "内容", "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(notice));

    assertThrows(BusinessException.class,
        () -> service.updateStatus(new PatientNoticeStatusRequest(1L, "INVALID"), 3L));
  }

  @Test
  void updateStatusDefaultsToEnabledWhenStatusIsNull() {
    PatientNotice notice = notice(1L, "公告", "内容", "DISABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(notice));
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Map<String, Object> row = service.updateStatus(new PatientNoticeStatusRequest(1L, null), 3L);

    assertEquals("ENABLED", row.get("status"));
  }

  // ── delete ─────────────────────────────────────────────────

  @Test
  void statusAndDeleteRejectDeletedOrMissingRows() {
    PatientNotice deleted = notice(1L, "旧公告", "内容", "ENABLED", true);
    when(repository.findById(1L)).thenReturn(Optional.of(deleted));
    when(repository.findById(2L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.updateStatus(new PatientNoticeStatusRequest(1L, "DISABLED"), 7L));
    assertThrows(BusinessException.class, () -> service.delete(2L, 7L));
  }

  @Test
  void deleteMarksRowDeleted() {
    PatientNotice notice = notice(1L, "公告", "内容", "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(notice));
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.delete(1L, 8L);

    ArgumentCaptor<PatientNotice> captor = ArgumentCaptor.forClass(PatientNotice.class);
    verify(repository).save(captor.capture());
    assertEquals(true, captor.getValue().getDeleted());
    assertEquals(8L, captor.getValue().getUpdatedBy());
  }

  // ── sort ───────────────────────────────────────────────────

  @Test
  void sortUpdatesSortFieldForEachItem() {
    PatientNotice n1 = notice(1L, "公告一", "内容一", "ENABLED", false);
    PatientNotice n2 = notice(2L, "公告二", "内容二", "ENABLED", false);
    when(repository.findById(1L)).thenReturn(Optional.of(n1));
    when(repository.findById(2L)).thenReturn(Optional.of(n2));
    when(repository.save(any(PatientNotice.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc())
        .thenReturn(List.of(n1, n2));

    PatientNoticeSortRequest sortRequest = new PatientNoticeSortRequest(List.of(
        new PatientNoticeSortRequest.SortItem(1L, 10),
        new PatientNoticeSortRequest.SortItem(2L, 20)
    ));

    List<Map<String, Object>> rows = service.sort(sortRequest, 5L);

    assertEquals(2, rows.size());
    verify(repository).save(n1);
    verify(repository).save(n2);
    assertEquals(5L, n1.getUpdatedBy());
    assertEquals(10, n1.getSort());
    assertEquals(20, n2.getSort());
  }

  @Test
  void sortRejectsDeletedOrMissingItems() {
    PatientNotice deleted = notice(1L, "公告", "内容", "ENABLED", true);
    when(repository.findById(1L)).thenReturn(Optional.of(deleted));

    PatientNoticeSortRequest sortRequest = new PatientNoticeSortRequest(List.of(
        new PatientNoticeSortRequest.SortItem(1L, 10)
    ));

    assertThrows(BusinessException.class, () -> service.sort(sortRequest, 5L));
  }

  // ── view: null field handling ───────────────────────────────

  @Test
  void viewHandlesNullDatesAndNullFields() {
    PatientNotice notice = new PatientNotice();
    notice.setId(1L);
    notice.setTitle(null);
    notice.setContent(null);
    notice.setLinkType(null);
    notice.setLinkUrl(null);
    notice.setStartTime(null);
    notice.setEndTime(null);
    notice.setPinned(null);
    notice.setSort(null);
    notice.setStatus(null);
    notice.setDeleted(false);
    notice.setCreatedBy(null);
    notice.setUpdatedBy(null);
    notice.setCreatedAt(null);
    notice.setUpdatedAt(null);

    when(repository.findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc())
        .thenReturn(List.of(notice));

    List<Map<String, Object>> rows = service.adminList();

    Map<String, Object> row = rows.get(0);
    assertEquals("", row.get("title"));
    assertEquals("", row.get("content"));
    assertEquals("NONE", row.get("linkType"));
    assertEquals("", row.get("linkUrl"));
    assertEquals("", row.get("startTime"));
    assertEquals("", row.get("endTime"));
    assertEquals(false, row.get("pinned"));
    assertEquals(0, row.get("sort"));
    assertEquals("ENABLED", row.get("status"));
    assertEquals("", row.get("createdAt"));
    assertEquals("", row.get("updatedAt"));
  }

  @Test
  void viewFormatsDatesWhenPresent() {
    PatientNotice notice = notice(1L, "公告", "内容", "ENABLED", false);
    LocalDateTime now = LocalDateTime.of(2026, 7, 1, 12, 0);
    notice.setStartTime(now);
    notice.setEndTime(now.plusDays(7));
    notice.setCreatedAt(now.minusDays(1));
    notice.setUpdatedAt(now);
    notice.setCreatedBy(10L);
    notice.setUpdatedBy(20L);

    when(repository.findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc())
        .thenReturn(List.of(notice));

    Map<String, Object> row = service.adminList().get(0);

    assertEquals(now.toString(), row.get("startTime"));
    assertEquals(now.plusDays(7).toString(), row.get("endTime"));
    assertEquals(now.minusDays(1).toString(), row.get("createdAt"));
    assertEquals(now.toString(), row.get("updatedAt"));
    assertEquals(10L, row.get("createdBy"));
    assertEquals(20L, row.get("updatedBy"));
  }

  // ── helpers ────────────────────────────────────────────────

  private PatientNotice notice(Long id, String title, String content, String status, boolean deleted) {
    PatientNotice notice = new PatientNotice();
    notice.setId(id);
    notice.setTitle(title);
    notice.setContent(content);
    notice.setLinkType("NONE");
    notice.setStatus(status);
    notice.setDeleted(deleted);
    notice.setPinned(false);
    notice.setSort(0);
    return notice;
  }
}
