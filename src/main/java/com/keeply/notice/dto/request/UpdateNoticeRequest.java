package com.keeply.notice.dto.request;

import com.keeply.notice.entity.NoticeTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateNoticeRequest {
  private String title;
  private String content;
  private NoticeTag tag;
  private String imageUrl;

  public boolean hsaUpdateField() {
    return title != null || content != null || tag != null || imageUrl != null;
  }
}
