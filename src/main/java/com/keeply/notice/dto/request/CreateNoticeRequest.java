package com.keeply.notice.dto.request;

import com.keeply.notice.entity.NoticeTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateNoticeRequest {
  @NotBlank(message = "공지 제목은 필수입니다.")
  private String title;

  @NotBlank(message = "공지 내용은 필수입니다.")
  private String content;

  @NotNull(message = "공지 태그는 필수입니다.")
  private NoticeTag tag;

  private String imageUrl;
}
