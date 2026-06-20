package com.smartcloudbrain.triage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.triage.entity.TriageRecord;
import com.smartcloudbrain.triage.repository.TriageRecordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriageDeskServiceTest {

  @Mock private TriageRecordRepository triageRecordRepository;
  @InjectMocks private TriageDeskService triageDeskService;

  @Test
  void listsAndShowsDetails() {
    TriageRecord record = record(5L);
    when(triageRecordRepository.findAllByOrderByIdDesc()).thenReturn(List.of(record));
    when(triageRecordRepository.findById(5L)).thenReturn(Optional.of(record));

    assertEquals(1, triageDeskService.list().size());
    assertEquals(5L, triageDeskService.detail(5L).get("triageRecordId"));
  }

  @Test
  void assignsDoctorAndClosesRecord() {
    TriageRecord record = record(5L);
    when(triageRecordRepository.findById(5L)).thenReturn(Optional.of(record));
    when(triageRecordRepository.save(record)).thenReturn(record);

    assertEquals("ASSIGNED", triageDeskService.assign(5L, 8L).get("status"));
    assertEquals(8L, record.getAssignedDoctorId());
    assertEquals("CLOSED", triageDeskService.close(5L).get("status"));
    verify(triageRecordRepository, times(2)).save(record);
  }

  @Test
  void throwsWhenRecordMissing() {
    when(triageRecordRepository.findById(404L)).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> triageDeskService.detail(404L));
    assertThrows(BusinessException.class, () -> triageDeskService.assign(404L, 1L));
    assertThrows(BusinessException.class, () -> triageDeskService.close(404L));
  }

  private static TriageRecord record(Long id) {
    TriageRecord record = new TriageRecord();
    record.setId(id);
    record.setPatientId(1L);
    record.setChiefComplaint("fever");
    record.setRecommendedDepartment("General");
    record.setStatus("AI_RECOMMENDED");
    return record;
  }
}
