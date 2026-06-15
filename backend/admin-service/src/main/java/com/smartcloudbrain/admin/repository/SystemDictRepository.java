package com.smartcloudbrain.admin.repository;

import com.smartcloudbrain.admin.entity.SystemDict;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemDictRepository extends JpaRepository<SystemDict, Long> {
  List<SystemDict> findByDictTypeOrderBySortAscIdAsc(String dictType);
}
