package com.smartcloudbrain.common.exception;

import com.smartcloudbrain.common.error.ErrorCode;

public class BusinessException extends RuntimeException {

  private final int code;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.message());
    this.code = errorCode.code();
  }

  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }

  public int code() {
    return code;
  }
}
