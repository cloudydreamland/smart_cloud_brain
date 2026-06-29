package com.smartcloudbrain.admin.service;

import com.smartcloudbrain.admin.dto.admin.PatientNoticeSaveRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeSortRequest;
import com.smartcloudbrain.admin.dto.admin.PatientNoticeStatusRequest;
import com.smartcloudbrain.admin.entity.PatientNotice;
import com.smartcloudbrain.admin.repository.PatientNoticeRepository;
import com.smartcloudbrain.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientNoticeService {

  private static final String STATUS_ENABLED = "ENABLED";
  private static final String STATUS_DISABLED = "DISABLED";
  private static final String LINK_NONE = "NONE";
  private static final List<String> LINK_TYPES = List.of(LINK_NONE, "INTERNAL", "EXTERNAL");

  private final PatientNoticeRepository repository;

  public PatientNoticeService(PatientNoticeRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminList() {
    return repository.findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc().stream()
        .map(this::view)
        .toList();
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "patientSite", key = "'notices'")
  public List<Map<String, Object>> publicList() {
    return repository.findPublicNotices(LocalDateTime.now()).stream()
        .map(this::view)
        .toList();
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public Map<String, Object> save(PatientNoticeSaveRequest request, Long userId) {
    PatientNotice notice = request.id() == null
        ? new PatientNotice()
        : repository.findById(request.id()).filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
            .orElseThrow(() -> new BusinessException(404, "Notice not found"));
    if (request.endTime() != null && request.startTime() != null && request.endTime().isBefore(request.startTime())) {
      throw new BusinessException(400, "公告失效时间不能早于生效时间");
    }
    notice.setTitle(required(request.title(), "公告标题不能为空"));
    notice.setContent(required(request.content(), "公告内容不能为空"));
    notice.setLinkType(normalizeLinkType(request.linkType()));
    notice.setLinkUrl(blankToNull(request.linkUrl()));
    notice.setStartTime(request.startTime());
    notice.setEndTime(request.endTime());
    notice.setPinned(Boolean.TRUE.equals(request.pinned()));
    notice.setSort(request.sort() == null ? 0 : request.sort());
    notice.setStatus(normalizeStatus(request.status()));
    notice.setDeleted(false);
    notice.setUpdatedBy(userId);
    notice.setUpdatedAt(LocalDateTime.now());
    if (notice.getId() == null) {
      notice.setCreatedBy(userId);
    }
    return view(repository.save(notice));
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public Map<String, Object> updateStatus(PatientNoticeStatusRequest request, Long userId) {
    PatientNotice notice = activeNotice(request.id());
    notice.setStatus(normalizeStatus(request.status()));
    notice.setUpdatedBy(userId);
    notice.setUpdatedAt(LocalDateTime.now());
    return view(repository.save(notice));
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public Map<String, Object> delete(Long id, Long userId) {
    PatientNotice notice = activeNotice(id);
    notice.setDeleted(true);
    notice.setUpdatedBy(userId);
    notice.setUpdatedAt(LocalDateTime.now());
    return view(repository.save(notice));
  }

  @Transactional
  @CacheEvict(cacheNames = "patientSite", allEntries = true)
  public List<Map<String, Object>> sort(PatientNoticeSortRequest request, Long userId) {
    for (PatientNoticeSortRequest.SortItem item : request.items()) {
      PatientNotice notice = activeNotice(item.id());
      notice.setSort(item.sort());
      notice.setUpdatedBy(userId);
      notice.setUpdatedAt(LocalDateTime.now());
      repository.save(notice);
    }
    return adminList();
  }

  private PatientNotice activeNotice(Long id) {
    return repository.findById(id)
        .filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
        .orElseThrow(() -> new BusinessException(404, "Notice not found"));
  }

  private Map<String, Object> view(PatientNotice notice) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", notice.getId());
    row.put("title", safe(notice.getTitle()));
    row.put("content", safe(notice.getContent()));
    row.put("linkType", safe(notice.getLinkType(), LINK_NONE));
    row.put("linkUrl", safe(notice.getLinkUrl()));
    row.put("startTime", notice.getStartTime() == null ? "" : notice.getStartTime().toString());
    row.put("endTime", notice.getEndTime() == null ? "" : notice.getEndTime().toString());
    row.put("pinned", Boolean.TRUE.equals(notice.getPinned()));
    row.put("sort", notice.getSort() == null ? 0 : notice.getSort());
    row.put("status", safe(notice.getStatus(), STATUS_ENABLED));
    row.put("createdBy", notice.getCreatedBy());
    row.put("updatedBy", notice.getUpdatedBy());
    row.put("createdAt", notice.getCreatedAt() == null ? "" : notice.getCreatedAt().toString());
    row.put("updatedAt", notice.getUpdatedAt() == null ? "" : notice.getUpdatedAt().toString());
    return row;
  }

  private String normalizeStatus(String status) {
    String value = safe(status, STATUS_ENABLED).toUpperCase();
    if (!STATUS_ENABLED.equals(value) && !STATUS_DISABLED.equals(value)) {
      throw new BusinessException(400, "Unsupported notice status: " + status);
    }
    return value;
  }

  private String normalizeLinkType(String linkType) {
    String value = safe(linkType, LINK_NONE).toUpperCase();
    if (!LINK_TYPES.contains(value)) {
      throw new BusinessException(400, "Unsupported notice linkType: " + linkType);
    }
    return value;
  }

  private String required(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(400, message);
    }
    return value.trim();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String safe(String value) {
    return safe(value, "");
  }

  private String safe(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
