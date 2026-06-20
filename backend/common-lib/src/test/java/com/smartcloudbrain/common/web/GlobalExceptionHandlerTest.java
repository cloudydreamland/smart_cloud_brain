package com.smartcloudbrain.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handlesBusinessException() {
    Result<Void> result = handler.business(new BusinessException(ErrorCode.FORBIDDEN));

    assertEquals(403, result.code());
    assertEquals("forbidden", result.message());
  }

  @Test
  void handlesValidationException() {
    Result<Void> result = handler.validation(new HttpMessageNotReadableException("bad"));

    assertEquals(400, result.code());
    assertEquals("invalid request parameters", result.message());
  }

  @Test
  void handlesSystemException() {
    Result<Void> result = handler.system(new IllegalStateException("boom"));

    assertEquals(500, result.code());
    assertEquals("internal server error", result.message());
  }
}
