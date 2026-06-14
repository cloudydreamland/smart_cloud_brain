package com.smartcloudbrain.common.error;

public enum ErrorCode {
  SUCCESS(0, "success"),
  BAD_REQUEST(400, "invalid request parameters"),
  UNAUTHORIZED(401, "unauthorized"),
  FORBIDDEN(403, "forbidden"),
  NOT_FOUND(404, "resource not found"),
  CONFLICT(409, "data conflict"),
  INTERNAL_ERROR(500, "internal server error"),
  AI_UNAVAILABLE(600, "AI service unavailable");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }
}
