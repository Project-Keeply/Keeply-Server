package com.keeply.notice.dto.request;

import com.keeply.notice.entity.NoticeTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateNoticeRequest {
  @Schema(description = "공지 제목", example = "신상품 입고 안내 수정")
  private String title;

  @Schema(description = "공지 내용", example = "신상품 진열 위치가 변경되었습니다.")
  private String content;

  @Schema(description = "공지 태그", example = "WEEKLY")
  private NoticeTag tag;

  @Schema(description = "첨부 이미지 URL", example = "https://example.com/notices/updated-image.png")
  private String imageUrl;

  @Schema(description = "기존 첨부 이미지 제거 여부", example = "false")
  private Boolean removeImage;

  public boolean hasUpdateField() {
    return title != null
        || content != null
        || tag != null
        || imageUrl != null
        || Boolean.TRUE.equals(removeImage);
  }

  public boolean hasBlankField() {
    return (title != null && title.isBlank()) || (content != null && content.isBlank());
  }

  public boolean hasImageConflict() {
    return Boolean.TRUE.equals(removeImage) && imageUrl != null;
  }

  public boolean isRemoveImage() {
    return Boolean.TRUE.equals(removeImage);
  }
}
