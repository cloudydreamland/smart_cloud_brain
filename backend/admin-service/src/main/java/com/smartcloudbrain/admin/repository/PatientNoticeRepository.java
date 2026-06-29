package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.PatientNotice;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientNoticeRepository extends JpaRepository<PatientNotice, Long> {

  List<PatientNotice> findByDeletedFalseOrderByPinnedDescSortAscUpdatedAtDesc();

  @Query("""
      select notice from PatientNotice notice
      where notice.deleted = false
        and notice.status = 'ENABLED'
        and (notice.startTime is null or notice.startTime <= :now)
        and (notice.endTime is null or notice.endTime >= :now)
      order by notice.pinned desc, notice.sort asc, notice.updatedAt desc, notice.id desc
      """)
  List<PatientNotice> findPublicNotices(@Param("now") LocalDateTime now);
}
