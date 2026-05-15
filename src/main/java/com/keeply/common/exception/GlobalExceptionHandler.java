package com.keeply.common.exception;

import com.keeply.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.failure(errorCode.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .findFirst()
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
        .body(ApiResponse.failure(message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
      ConstraintViolationException e) {
    String message =
        e.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .findFirst()
            .orElse(ErrorCode.INVALID_INPUT.getMessage());
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
        .body(ApiResponse.failure(message));
  }
}
