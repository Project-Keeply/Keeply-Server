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
  FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_003", "해당 리소스에 접근 권한이 없습니다."),

  // User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "존재하지 않는 유저입니다."),

  // Group
  USER_ALREADY_IN_GROUP(HttpStatus.CONFLICT, "GROUP_001", "이미 그룹에 소속된 유저입니다."),
  INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GROUP_002", "초대코드 생성에 실패했습니다."),
  INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_003", "유효하지 않은 초대코드입니다.");

  @NonNull private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
