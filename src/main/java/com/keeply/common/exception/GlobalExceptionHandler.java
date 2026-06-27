package com.keeply.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.keeply.common.response.ApiResponse;
import com.keeply.file.domain.FileDomain;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e) {
    ErrorCode errorCode =
        FileDomain.class.equals(e.getRequiredType())
            ? ErrorCode.FILE_INVALID_DOMAIN
            : ErrorCode.INVALID_INPUT;
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.failure(errorCode.getMessage()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    ErrorCode errorCode = resolveHttpMessageErrorCode(e);
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.failure(errorCode.getMessage()));
  }

  private ErrorCode resolveHttpMessageErrorCode(HttpMessageNotReadableException e) {
    if (e.getCause() instanceof InvalidFormatException ife
        && FileDomain.class.equals(ife.getTargetType())) {
      return ErrorCode.FILE_INVALID_DOMAIN;
    }
    return ErrorCode.INVALID_INPUT;
  }

  @ExceptionHandler({JwtException.class, NumberFormatException.class})
  public ResponseEntity<ApiResponse<Void>> handleJwtException(Exception e) {
    return ResponseEntity.status(ErrorCode.INVALID_TOKEN.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INVALID_TOKEN.getMessage()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
      AuthenticationException e) {
    return ResponseEntity.status(ErrorCode.INVALID_TOKEN.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INVALID_TOKEN.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
    return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.FORBIDDEN.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
      DataIntegrityViolationException e) {
    log.warn("Data integrity violation", e);
    ErrorCode errorCode = resolveDataIntegrityErrorCode(e);
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.failure(errorCode.getMessage()));
  }

  private ErrorCode resolveDataIntegrityErrorCode(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    if (cause == null || cause.getMessage() == null) {
      return ErrorCode.INTERNAL_SERVER_ERROR;
    }
    String message = cause.getMessage().toLowerCase();
    if (message.contains("group_members")) {
      return ErrorCode.USER_ALREADY_IN_GROUP;
    }
    if (message.contains("invite_code")) {
      return ErrorCode.INVITE_CODE_GENERATION_FAILED;
    }
    return ErrorCode.INTERNAL_SERVER_ERROR;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
    log.error("Unexpected error", e);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.failure(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
  }
}
