package com.smartcloudbrain.diagnosis.exception;

import com.smartcloudbrain.common.result.Result;
import com.smartcloudbrain.common.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.OK)
  public Result<?> handleBusinessException(BusinessException ex) {
    return Result.failure(ex.code(), ex.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleValidationException(Exception ex) {
    return Result.failure(400, "invalid request parameters");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<?> handleException(Exception ex) {
    return Result.failure(500, "internal server error");
  }
}
