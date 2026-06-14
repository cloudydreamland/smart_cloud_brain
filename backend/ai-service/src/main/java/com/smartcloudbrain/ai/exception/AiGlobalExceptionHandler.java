package com.smartcloudbrain.ai.exception;

import com.smartcloudbrain.common.error.ErrorCode;
import com.smartcloudbrain.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiGlobalExceptionHandler {

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<?> handleValidationException(Exception ex) {
    return Result.failure(ErrorCode.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<?> handleException(Exception ex) {
    return Result.failure(ErrorCode.AI_UNAVAILABLE);
  }
}
