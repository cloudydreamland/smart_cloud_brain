package com.smartcloudbrain.common.web;

import com.smartcloudbrain.common.exception.BusinessException;
import com.smartcloudbrain.common.result.Result;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public Result<Void> business(BusinessException ex) {
    return Result.failure(ex.code(), ex.getMessage());
  }

  @ExceptionHandler({
      MethodArgumentNotValidException.class,
      BindException.class,
      HttpMessageNotReadableException.class
  })
  public Result<Void> validation(Exception ex) {
    return Result.failure(400, "invalid request parameters");
  }

  @ExceptionHandler(Exception.class)
  public Result<Void> system(Exception ex) {
    ex.printStackTrace(System.err);
    return Result.failure(500, "internal server error");
  }
}
