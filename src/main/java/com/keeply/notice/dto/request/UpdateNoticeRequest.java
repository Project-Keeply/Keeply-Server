package com.keeply.notice.dto.request;

import com.keeply.notice.entity.NoticeTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
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

  @AssertTrue(message = "수정할 항목이 최소 하나 이상 있어야 합니다.")
  public boolean isUpdateFieldPresent() {
    return title != null
        || content != null
        || tag != null
        || imageUrl != null
        || Boolean.TRUE.equals(removeImage);
  }

  @AssertTrue(message = "제목/내용/이미지 URL은 공백일 수 없습니다.")
  public boolean isFieldNotBlank() {
    return !((title != null && title.isBlank())
        || (content != null && content.isBlank())
        || (imageUrl != null && imageUrl.isBlank()));
  }

  @AssertTrue(message = "이미지 제거와 이미지 URL 설정은 동시에 할 수 없습니다.")
  public boolean isImageConflictFree() {
    return !(Boolean.TRUE.equals(removeImage) && imageUrl != null);
  }

  public boolean isRemoveImage() {
    return Boolean.TRUE.equals(removeImage);
  }
}
