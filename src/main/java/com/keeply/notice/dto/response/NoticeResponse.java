package com.keeply.notice.dto.response;

import com.keeply.notice.domain.NoticeDisplayPeriod;
import com.keeply.notice.entity.Notice;
import com.keeply.notice.entity.NoticeTag;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NoticeResponse {

  @Schema(description = "공지 ID", example = "1")
  private final Long noticeId;

  @Schema(description = "공지 제목", example = "신상품 입고 안내")
  private final String title;

  @Schema(description = "공지 내용", example = "오늘 야간 근무자는 신상품 진열 상태를 확인해주세요.")
  private final String content;

  @Schema(description = "공지 태그", example = "DAILY")
  private final NoticeTag tag;

  @Schema(description = "첨부 이미지 URL", example = "https://example.com/notices/image.png")
  private final String imageUrl;

  @Schema(description = "작성자 유저 ID", example = "1")
  private final Long authorUserId;

  @Schema(description = "작성자 이름", example = "홍길동")
  private final String authorName;

  @Schema(description = "작성 시각", example = "2026-07-02T10:30:00")
  private final LocalDateTime createdAt;

  @Schema(description = "표시 시작 시각", example = "2026-07-02T00:00:00")
  private final LocalDateTime displayStartAt;

  @Schema(description = "표시 종료 시각, exclusive 기준", example = "2026-07-03T00:00:00")
  private final LocalDateTime displayEndAt;

  public static NoticeResponse of(Notice notice) {
    NoticeDisplayPeriod displayPeriod = NoticeDisplayPeriod.from(notice);
    return NoticeResponse.builder()
        .noticeId(notice.getId())
        .title(notice.getTitle())
        .content(notice.getContent())
        .tag(notice.getTag())
        .imageUrl(notice.getImageUrl())
        .authorUserId(notice.getAuthorMember().getUser().getId())
        .authorName(notice.getAuthorMember().getUser().getName())
        .createdAt(notice.getCreatedAt())
        .displayStartAt(displayPeriod.startAt())
        .displayEndAt(displayPeriod.endAt())
        .build();
  }
}
