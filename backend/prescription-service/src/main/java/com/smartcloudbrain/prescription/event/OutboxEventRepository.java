package com.smartcloudbrain.prescription.event;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  List<OutboxEvent> findTop20ByStatusInAndNextRetryAtLessThanEqualOrderByIdAsc(
      Collection<String> statuses,
      LocalDateTime nextRetryAt
  );
}
