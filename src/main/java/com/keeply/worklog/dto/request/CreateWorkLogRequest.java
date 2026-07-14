package com.keeply.worklog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateWorkLogRequest {

  @Schema(description = "운영 로그 내용", example = "냉장고 온도가 일시적으로 높아져 점검 요청했습니다.")
  @NotBlank(message = "운영 로그 내용은 필수입니다.")
  private String content;
}
