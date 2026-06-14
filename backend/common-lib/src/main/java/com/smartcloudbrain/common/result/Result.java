package com.smartcloudbrain.common.result;

import com.smartcloudbrain.common.error.ErrorCode;

public record Result<T>(int code, String message, T data) {

  public static <T> Result<T> success(T data) {
    return new Result<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data);
  }

  public static <T> Result<T> failure(ErrorCode errorCode) {
    return new Result<>(errorCode.code(), errorCode.message(), null);
  }

  public static <T> Result<T> failure(int code, String message) {
    return new Result<>(code, message, null);
  }
}
