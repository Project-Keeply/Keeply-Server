package com.keeply.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
  private final boolean success;
  private final T data;
  private final String message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(
      description = "오류 식별 코드. 성공 응답에서는 생략됩니다.",
      example = "REFRESH_TOKEN_EXPIRED",
      nullable = true)
  private final String code;

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> failure(String message) {
    return new ApiResponse<>(false, null, message, null);
  }

  public static <T> ApiResponse<T> failure(String message, String code) {
    return new ApiResponse<>(false, null, message, code);
  }
}
