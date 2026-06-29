package com.smartcloudbrain.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcloudbrain.admin.dto.admin.PatientNoticeSaveRequest;
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
