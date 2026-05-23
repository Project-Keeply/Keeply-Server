package com.keeply.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  // Common
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생하였습니다."),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_002", "입력값이 올바르지 않습니다."),

  // Auth
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다."),
  KAKAO_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "AUTH_002", "카카오 인증에 실패하였습니다."),

  // User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 유저입니다.");

  @NonNull private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
