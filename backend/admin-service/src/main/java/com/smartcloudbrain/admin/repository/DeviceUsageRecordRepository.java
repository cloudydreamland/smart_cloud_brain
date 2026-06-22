package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.DeviceUsageRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceUsageRecordRepository extends JpaRepository<DeviceUsageRecord, Long> {
  List<DeviceUsageRecord> findByDeviceIdOrderByStartedAtDescIdDesc(Long deviceId);
}
