package com.keeply.worklog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateWorkLogRequest {

  @Schema(description = "운영 로그 내용", example = "냉장고 점검 완료, 현재 온도 정상입니다.")
  private String content;

  @AssertTrue(message = "수정할 항목이 최소 하나 이상 있어야 합니다.")
  public boolean isUpdateFieldPresent() {
    return content != null;
  }

  @AssertTrue(message = "운영 로그 내용은 공백일 수 없습니다.")
  public boolean isContentNotBlank() {
    return content == null || !content.isBlank();
  }
}
