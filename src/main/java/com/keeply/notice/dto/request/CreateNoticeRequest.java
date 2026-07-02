package com.keeply.notice.dto.request;

import com.keeply.notice.entity.NoticeTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateNoticeRequest {
  @Schema(description = "공지 제목", example = "신상품 입고 안내")
  @NotBlank(message = "공지 제목은 필수입니다.")
  private String title;

  @Schema(description = "공지 내용", example = "오늘 야간 근무자는 신상품 진열 상태를 확인해주세요.")
  @NotBlank(message = "공지 내용은 필수입니다.")
  private String content;

  @Schema(description = "공지 태그", example = "DAILY")
  @NotNull(message = "공지 태그는 필수입니다.")
  private NoticeTag tag;

  @Schema(description = "첨부 이미지 URL", example = "https://example.com/notices/image.png")
  private String imageUrl;
}
