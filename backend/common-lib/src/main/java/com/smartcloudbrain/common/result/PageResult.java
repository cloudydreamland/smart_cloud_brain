package com.smartcloudbrain.common.result;

import java.util.List;

public record PageResult<T>(
    List<T> records,
    long total,
    int pageNo,
    int pageSize
) {

  public static <T> PageResult<T> empty(int pageNo, int pageSize) {
    return new PageResult<>(List.of(), 0, pageNo, pageSize);
  }
}
